/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.server.chunkio.region;

import cubicchunks.regionlib.MultiUnsupportedDataException;
import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.lib.Region;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import cubicchunks.regionlib.lib.header.IKeyIdToSectorMap;
import cubicchunks.regionlib.lib.header.IntPackedSectorMap;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CorruptedDataException;
import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import net.minecraft.util.Tuple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.*;

/**
 * Simplified {@link Region} class implementing shadow paging by using custom sector tracker
 * that doesn't reallocate entries in place.
 */
public class ShadowPagingRegion<K extends IKey<K>> implements IRegion<K> {
	//a read-only global buffer containing zeroes, used as a read source when zeroing out sector contents
	private static final int ZERO_BYTEBUFFER_CAPACITY = 4096;
	private static final ByteBuffer ZERO_BYTEBUFFER = ByteBuffer.allocateDirect(ZERO_BYTEBUFFER_CAPACITY).asReadOnlyBuffer();

	/**
	 * Gets an array of read-only {@link ByteBuffer}(s) filled with zeroes, whose total {@link ByteBuffer#remaining() remaining} space is equal to
	 * the given {@code length}.
	 *
	 * @param length the number of zero bytes
	 * @return an array of read-only {@link ByteBuffer}(s) filled with zeroes
	 */
	private static ByteBuffer[] zeroes(int length) {
		ByteBuffer[] arr = new ByteBuffer[Math.floorDiv(length - 1, ZERO_BYTEBUFFER_CAPACITY) + 1];
		for (int i = 0; i < arr.length; i++) {
			int remaining = length - i * ZERO_BYTEBUFFER_CAPACITY;
			arr[i] = (ByteBuffer) ZERO_BYTEBUFFER.duplicate().clear().limit(Math.min(remaining, ZERO_BYTEBUFFER_CAPACITY));
		}
		return arr;
	}

	/**
	 * Adds read-only {@link ByteBuffer}(s) filled with zeroes to the given {@link List}, whose total {@link ByteBuffer#remaining() remaining} space
	 * is equal to the given {@code length}.
	 *
	 * @param length the number of zero bytes
	 * @param target the {@link List} to add the {@link ByteBuffer}(s) to
	 */
	private static void zeroes(int length, List<ByteBuffer> target) {
		for (int i = 0, count = Math.floorDiv(length - 1, ZERO_BYTEBUFFER_CAPACITY) + 1; i < count; i++) {
			int remaining = length - i * ZERO_BYTEBUFFER_CAPACITY;
			target.add((ByteBuffer) ZERO_BYTEBUFFER.duplicate().clear().limit(Math.min(remaining, ZERO_BYTEBUFFER_CAPACITY)));
		}
	}

	private final FileChannel file;
	private final IHeaderDataEntryProvider<?, K> headerEntryProvider;
	private final RegionKey regionKey;
	private final IKeyProvider<K> keyProvider;
	private final int sectorSize;
	private final SectorTracker<K> sectorTracker;

	private final ReadWriteLock dataLock = new ReentrantReadWriteLock();
	private final ReadWriteLock reserveSectorsLock = new ReentrantReadWriteLock();
	private ShadowPagingRegion(FileChannel file, SectorTracker<K> sectorTracker, IHeaderDataEntryProvider<?, K> headerEntryProvider, RegionKey regionKey, IKeyProvider<K> keyProvider, int sectorSize) {
		this.file = file;
		this.headerEntryProvider = headerEntryProvider;
		this.regionKey = regionKey;
		this.keyProvider = keyProvider;
		this.sectorSize = sectorSize;
		this.sectorTracker = sectorTracker;
	}

	@Override
	public void writeValue(K key, ByteBuffer value) throws IOException {
		CubicChunks.LOGGER.warn("Using slow non-batch write at {} in {}", key, this.regionKey.getName());
		this.writeValues(Collections.singletonMap(key, value));
	}

	@Override
	public void writeValues(Map<K, ByteBuffer> entries) throws IOException {
		if (entries.isEmpty()) { //fast-path if there isn't anything to be written
			return;
		}
		//calling file.force() is slow, so we want to minimize the number of times it needs to be called. the solution is simple: we write the data for ALL
		//  entries at once, and don't update the headers until it's all been written to disk.
		List<UnsupportedDataException.WithKey> exceptions = new ArrayList<>();
		Map<K, Optional<RegionEntryLocation>> pendingHeaderUpdates = new HashMap<>(entries.size());
		Map<K, RegionEntryLocation> entryLocationsToUse = new HashMap<>(entries.size());

		Lock sectorLock = reserveSectorsLock.writeLock();
		Lock mainLock = dataLock.writeLock();
		sectorLock.lock();
		mainLock.lock();
		//entries.forEach((k, v) -> CubicChunks.LOGGER.error(this + ": WRITE: " + k + ", " + v.remaining()));
		try {
			// first pass: reserve header locations:
			reserveHeaderEntriesPass(entries, exceptions, pendingHeaderUpdates, entryLocationsToUse);
		} finally {
			sectorLock.unlock();
		}
		try {
			// second pass: write all data
			boolean shouldFlush = writeDataPass(entries, exceptions, entryLocationsToUse);

			//flush the file's contents if any of the entries modified the region data
			if (shouldFlush) {
				this.file.force(true);
			}

			// third pass: execute pending header updates
			doPendingHeaderUpdatesPass(pendingHeaderUpdates);

			//throw all pending exceptions at once if any occurred
			if (!exceptions.isEmpty()) {
				throw new MultiUnsupportedDataException(exceptions);
			}
		} finally {
			 mainLock.unlock();
		}
	}

	private void reserveHeaderEntriesPass(Map<K, ByteBuffer> entries, List<UnsupportedDataException.WithKey> exceptions,
			Map<K, Optional<RegionEntryLocation>> pendingHeaderUpdates, Map<K, RegionEntryLocation> entryLocationsToUse) throws IOException {
		for (Iterator<Map.Entry<K, ByteBuffer>> itr = entries.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry<K, ByteBuffer> entry = itr.next();

			K key = entry.getKey();
			ByteBuffer value = entry.getValue();

			try {
				Optional<RegionEntryLocation> prevLocation;
				if (value == null) {
					//if deleting an entry, there's no need to change anything on disk! the only thing that needs
					// to be changed is the headers.
					prevLocation = null;
				} else {
					int size = value.remaining();
					int sizeWithSizeInfo = size + Integer.BYTES;
					int numSectors = this.getSectorNumber(sizeWithSizeInfo);

					//this may throw UnsupportedDataException if data is too big.
					//it won't cause the sector tracker to be updated, meaning that reallocated sectors won't be overwritten by
					// subsequent writes from the same batch.
					Tuple<RegionEntryLocation, RegionEntryLocation> headerUpdate = this.sectorTracker.reserveForKey(key, numSectors);
					prevLocation = Optional.ofNullable(headerUpdate.getFirst());
					entryLocationsToUse.put(key, headerUpdate.getSecond());
				}
				pendingHeaderUpdates.put(key, prevLocation);
			} catch (UnsupportedDataException e) {
				//save exception for later
				exceptions.add(new UnsupportedDataException.WithKey(e, key));
			}
		}
	}

	private boolean writeDataPass(Map<K, ByteBuffer> entries, List<UnsupportedDataException.WithKey> exceptions,
			Map<K, RegionEntryLocation> entryLocationsToUse) throws IOException {
		boolean shouldFlush = false;

		List<ByteBuffer> tempBuffers = new ArrayList<>();
		ByteBuffer lengthPrefixBuffer = ByteBuffer.allocate(Integer.BYTES);

		for (Iterator<Map.Entry<K, ByteBuffer>> itr = entries.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry<K, ByteBuffer> entry = itr.next();

			K key = entry.getKey();
			ByteBuffer value = entry.getValue();

			try {
				//if deleting an entry, there's no need to change anything on disk! the only thing that needs
				// to be changed is the headers.
				if (value != null) {
					int size = value.remaining();
					int bytesOffset = entryLocationsToUse.get(key).getOffset() * this.sectorSize;

					//build sequence of buffers for vectored IO
					tempBuffers.clear();
					tempBuffers.add(((ByteBuffer) lengthPrefixBuffer.clear()).putInt(0, size));
					tempBuffers.add(value);
					if ((lengthPrefixBuffer.capacity() + value.remaining()) % this.sectorSize != 0) { //pad trailing sector with zeroes
						zeroes(this.sectorSize - (lengthPrefixBuffer.capacity() + value.remaining()) % this.sectorSize, tempBuffers);
					}
					assert tempBuffers.stream().mapToInt(ByteBuffer::remaining).sum() == entryLocationsToUse.get(key).getSize() * this.sectorSize;

					//write all data to the entry's new location
					Utils.writeFully(this.file.position(bytesOffset), tempBuffers.toArray(new ByteBuffer[0]));

					//data has changed on disk, so we'll need to flush it before updating the headers
					shouldFlush = true;
				}
			} catch (UnsupportedDataException e) {
				//save exception for later
				exceptions.add(new UnsupportedDataException.WithKey(e, key));
			}
		}
		return shouldFlush;
	}

	private void doPendingHeaderUpdatesPass(Map<K, Optional<RegionEntryLocation>> pendingHeaderUpdates) throws IOException {
		if (!pendingHeaderUpdates.isEmpty()) {
			for (Iterator<Map.Entry<K, Optional<RegionEntryLocation>>> itr = pendingHeaderUpdates.entrySet().iterator(); itr.hasNext(); ) {
				Map.Entry<K, Optional<RegionEntryLocation>> entry = itr.next();

				K key = entry.getKey();
				Optional<RegionEntryLocation> prevLocation = entry.getValue();
				if (prevLocation == null) {
					//the entry is being deleted, so we need to remove the key from the headers entirely
					this.sectorTracker.removeKey(key);
				} else {
					//the entry changed, and we need to release the previous sectors
					this.sectorTracker.updateUsedSectorsFor(prevLocation.orElse(null), null);
				}

				//write new header value for this key to disk
				this.updateHeaders(key);
			}

			//ensure all header modifications are present on disk before another batch runs
			this.file.force(true);
		}
	}

	@Override public void writeSpecial(K key, Object marker) throws IOException {
		Lock sectorLock = reserveSectorsLock.writeLock();
		Lock mainLock = dataLock.writeLock();
		sectorLock.lock();
		mainLock.lock();
		try {
			this.sectorTracker.setSpecial(key, marker);
			updateHeaders(key);
			file.force(false);
		} finally {
			mainLock.unlock();
			sectorLock.unlock();
		}
	}

	private void updateHeaders(K key) throws IOException {
		int entryByteCount = headerEntryProvider.getEntryByteCount();
		ByteBuffer buf = ByteBuffer.allocate(entryByteCount);
		headerEntryProvider.apply(key).write(buf);
		buf.flip();
		Utils.writeFully(file.position((long) key.getId() * entryByteCount), buf);
	}

	@Override public Optional<ByteBuffer> readValue(K key) throws IOException {

		Lock sectorLock = reserveSectorsLock.readLock();
		Lock mainLock = dataLock.readLock();
		boolean mainLocked = false;
		try {
			sectorLock.lock();


			//CubicChunks.LOGGER.error(this + ": READ: " + key);
			Function<K, ByteBuffer> specialValue = sectorTracker.trySpecialValue(key).orElse(null);
			if (specialValue != null) {
				return Optional.of(specialValue.apply(key));
			}
			Optional<RegionEntryLocation> entryLocation = sectorTracker.getEntryLocation(key);
			if (!entryLocation.isPresent()) {
				return Optional.empty();
			}
			mainLock.lock();
			mainLocked = true;
			// get sector tracker entry again in case it got deleted in the meantime
			return doReadKey(key);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} finally {
			sectorLock.unlock();
			if (mainLocked) {
				mainLock.unlock();
			}
		}
	}

	private Optional<ByteBuffer> doReadKey(K key) {
		try {
			Optional<RegionEntryLocation> entryLocation = sectorTracker.getEntryLocation(key);
			if (!entryLocation.isPresent()) {
				return Optional.empty();
			}
			RegionEntryLocation loc = entryLocation.get();
			int sectorOffset = loc.getOffset();
			int sectorCount = loc.getSize();

			// read data size (one int)
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			long position = (long) sectorOffset * sectorSize;
			readFully(file, buf, position);

			int dataLength = buf.getInt(0);
			if (dataLength > sectorCount * sectorSize) {
				throw new CorruptedDataException(
						"Expected data size max " + sectorCount * sectorSize + " but found " + dataLength);
			}

			// read data
			ByteBuffer bytes = ByteBuffer.allocate(dataLength);
			readFully(file, bytes, position + Integer.BYTES);
			bytes.flip();
			return Optional.of(bytes);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns true if something was stored there before within this region.
	 */
	@Override public boolean hasValue(K key) {
		reserveSectorsLock.readLock().lock();
		try {
			return sectorTracker.trySpecialValue(key).isPresent() || sectorTracker.getEntryLocation(key).isPresent();
		} finally {
			reserveSectorsLock.readLock().unlock();
		}
	}

	@Override public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
		// acquire write locks even when we are "only" reading because callbacks may write
		reserveSectorsLock.writeLock().lock();
		dataLock.writeLock().lock();
		try {
			int keyCount = this.keyProvider.getKeyCount(regionKey);
			for (int id = 0; id < keyCount; id++) {
				int idFinal = id; // because java is stupid
				K key = sectorTracker.getEntryLocation(id).map(loc -> keyProvider.fromRegionAndId(this.regionKey, idFinal)).orElse(null);
				if (key != null) {
					cons.accept(key);
				}
			}
		} finally {
			dataLock.writeLock().unlock();
			reserveSectorsLock.writeLock().unlock();
		}
	}

	private int getSectorNumber(int bytes) {
		return ceilDiv(bytes, sectorSize);
	}

	@Override
	public void flush() throws IOException {
		//CubicChunks.bigWarning(this + ": FLUSH!!!");
		reserveSectorsLock.writeLock().lock();
		dataLock.writeLock().lock();
		try {
			boolean fileLengthChanged = false;
			fileLengthChanged |= this.ensureSectorSizeAligned();

			//Flushable declares that flush() must ensure that "any buffered output" is written. IMO, erasing sectors (an action typically deferred
			//  until the region file is closed) can be considered buffered output, so we'll deal with erasing them here
			fileLengthChanged |= this.erasePendingSectors();

			//if the file's length changed, we want to make sure we also force metadata updates to disk
			this.file.force(fileLengthChanged);
		} finally {
			dataLock.writeLock().unlock();
			reserveSectorsLock.writeLock().unlock();
		}
	}

	@Override public void close() throws IOException {
		//CubicChunks.bigWarning(this + ": CLOSE!!!");
		reserveSectorsLock.writeLock().lock();
		dataLock.writeLock().lock();

		//try-with-resources on file to ensure that the file gets closed, even if the other code throws an exception
		try (FileChannel file = this.file) {
			this.ensureSectorSizeAligned();
			this.erasePendingSectors();
		} finally {
			dataLock.writeLock().unlock();
			reserveSectorsLock.writeLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file's length was changed as a result of this operation, {@code false} otherwise
	 */
	private boolean ensureSectorSizeAligned() throws IOException {
		long fileSize = this.file.size();
		if (fileSize % sectorSize != 0) {
			//seek to EOF
			this.file.position(fileSize);

			//write enough zeroes to pad the file length to the next multiple of the sector size
			int extra = (int) (sectorSize - (fileSize % sectorSize));
			Utils.writeFully(this.file, zeroes(extra));

			assert this.file.size() % sectorSize == 0;
			return true; //the file's length changed
		}
		return false;
	}

	/**
	 * @return {@code true} if the file's length was changed as a result of this operation, {@code false} otherwise
	 */
	private boolean erasePendingSectors() throws IOException {
		//erase all the sectors which are pending erasure
		for (RegionEntryLocation range : this.sectorTracker.getAllSectorsPendingErasure()) {
			//seek to the beginning of the range, then fill it with zeroes
			this.file.position(range.getOffset() * (long) this.sectorSize);
			Utils.writeFully(this.file, zeroes(Math.multiplyExact(range.getSize(), this.sectorSize)));

			//inform the sector tracker that the sectors have been erased
			this.sectorTracker.markSectorsErased(range);
		}

		//consider truncating the file to trim unused sectors from the end
		long expectedFileSize = this.sectorTracker.getSectorsLength() * (long) this.sectorSize;
		long actualFileSize = this.file.size();
		assert expectedFileSize <= actualFileSize : "region file is too short???";
		if (actualFileSize > expectedFileSize) { //the file has unused sectors at the end, truncate it to save space
			this.file.truncate(expectedFileSize);
			return true; //the file's length changed
		}
		return false;
	}

	private static int ceilDiv(int x, int y) {
		return -Math.floorDiv(-x, y);
	}

	public static <L extends IKey<L>> ShadowPagingRegion.Builder<L> builder() {
		return new ShadowPagingRegion.Builder<>();
	}

	public static void readFully(FileChannel src, ByteBuffer data, long position) throws IOException {
		while (data.hasRemaining()) {
			src.read(data, position);
		}
	}

	public static class Builder<K extends IKey<K>> {

		private Path directory;
		private int sectorSize = 512;
		private RegionKey regionKey;
		private IKeyProvider<K> keyProvider;

		public Builder<K> setDirectory(Path path) {
			this.directory = path;
			return this;
		}

		public Builder<K> setRegionKey(RegionKey key) {
			this.regionKey = key;
			return this;
		}

		public Builder<K> setKeyProvider(IKeyProvider<K> keyProvider) {
			this.keyProvider = keyProvider;
			return this;
		}

		public Builder<K> setSectorSize(int sectorSize) {
			this.sectorSize = sectorSize;
			return this;
		}

		public ShadowPagingRegion<K> build() throws IOException {
			FileChannel file = FileChannel.open(directory.resolve(regionKey.getName()), CREATE, READ, WRITE);

			int entryMapBytes = Integer.BYTES;
			int entryMapSectors = ceilDiv(keyProvider.getKeyCount(regionKey) * entryMapBytes, sectorSize);

			IntPackedSectorMap<K> sectorMap = IntPackedSectorMap.readOrCreate(file, keyProvider.getKeyCount(regionKey), new ArrayList<>());
			SectorTracker<K> regionSectorTracker = SectorTracker.fromFile(file, sectorMap, entryMapSectors, sectorSize);
			return new ShadowPagingRegion<>(file, regionSectorTracker, sectorMap.headerEntryProvider(), this.regionKey, keyProvider, this.sectorSize);
		}
	}

	private static class SectorTracker<K extends IKey<K>> {

		private final BitSet usedSectors;
		private final IKeyIdToSectorMap<?, ?, K> sectorMap;

		/**
		 * Set of sectors which are queued to be zeroed out.
		 */
		private final BitSet sectorsPendingErasure = new BitSet();

		private SectorTracker(BitSet usedSectors, IKeyIdToSectorMap<?, ?, K> sectorMap) {
			this.usedSectors = usedSectors;
			this.sectorMap = sectorMap;
		}

		public Optional<RegionEntryLocation> getEntryLocation(int id) {
			return sectorMap.getEntryLocation(id);
		}

		public Optional<RegionEntryLocation> getEntryLocation(K key) {
			return sectorMap.getEntryLocation(key);
		}

		public void setSpecial(K key, Object obj) throws IOException {
			removeKey(key);
			sectorMap.setSpecial(key, obj);
		}

		public Optional<Function<K, ByteBuffer>> trySpecialValue(K key) {
			return sectorMap.trySpecialValue(key);
		}

		public void removeKey(K key) throws IOException {
			Optional<RegionEntryLocation> existing = sectorMap.getEntryLocation(key);
			RegionEntryLocation loc = new RegionEntryLocation(0, 0);
			this.sectorMap.setOffsetAndSize(key, loc);
			this.updateUsedSectorsFor(existing.orElse(null), loc);
		}

		/**
		 * Returns the old offset for the given key and the new offset for the given key and requestedSize, and reserves the new sectors.
		 *
		 * The old sectors will not be released.
		 */
		public Tuple<RegionEntryLocation, RegionEntryLocation> reserveForKey(K key, int requestedSize) throws IOException {
			Optional<RegionEntryLocation> existing = sectorMap.getEntryLocation(key);
			RegionEntryLocation found = findFree(requestedSize);
			this.sectorMap.setOffsetAndSize(key, found);
			this.updateUsedSectorsFor(null, found); //mark new sectors as allocated
			return new Tuple<>(existing.orElse(null), found);
		}

		private RegionEntryLocation findFree(int requestedSize) {
			int next = 0, current, runSize;
			do {
				int nextClear = usedSectors.nextClearBit(next);
				int nextUsed = usedSectors.nextSetBit(nextClear);
				current = nextClear;
				next = nextUsed;
				runSize = nextUsed < 0 ? Integer.MAX_VALUE : nextUsed - nextClear;
			} while (runSize < requestedSize);

			return new RegionEntryLocation(current, requestedSize);
		}

		private void updateUsedSectorsFor(RegionEntryLocation oldSectorLocation, RegionEntryLocation newSectorLocation) {
			if (oldSectorLocation != null) {
				int oldOffset = oldSectorLocation.getOffset();
				usedSectors.set(oldOffset, oldOffset + oldSectorLocation.getSize(), false);

				//the sectors are no longer used, we can mark them as free in order to zero them out later
				this.sectorsPendingErasure.set(oldOffset, oldOffset + oldSectorLocation.getSize(), true);
			}
			if (newSectorLocation != null) {
				int newOffset = newSectorLocation.getOffset();
				usedSectors.set(newOffset, newOffset + newSectorLocation.getSize(), true);

				//the sectors are now used, so we want to make sure they don't get zeroed out later
				this.sectorsPendingErasure.set(newOffset, newOffset + newSectorLocation.getSize(), false);
			}
		}

		public List<RegionEntryLocation> getAllSectorsPendingErasure() {
			List<RegionEntryLocation> out = new ArrayList<>();
			for (int next = 0; (next = this.sectorsPendingErasure.nextSetBit(next)) >= 0; ) {
				int rangeStart = next;
				int rangeEnd = this.sectorsPendingErasure.nextClearBit(rangeStart);
				out.add(new RegionEntryLocation(rangeStart, rangeEnd - rangeStart));

				next = rangeEnd;
			}
			return out;
		}

		public void markSectorsErased(RegionEntryLocation range) {
			assert this.sectorsPendingErasure.get(range.getOffset()) && this.sectorsPendingErasure.nextClearBit(range.getOffset()) == range.getSize() + range.getOffset()
					: "the given range isn't pending erasure";

			this.sectorsPendingErasure.clear(range.getOffset(), range.getOffset() + range.getSize());
		}

		public int getSectorsLength() {
			return this.usedSectors.length();
		}

		private boolean isSectorFree(int sector) {
			return !usedSectors.get(sector);
		}

		public static <L extends IKey<L>> SectorTracker<L> fromFile(
				SeekableByteChannel file, IKeyIdToSectorMap<?, ?, L> sectorMap, int reservedSectors, int sectorSize) throws IOException {
			// initialize usedSectors and make the header sectors as used
			BitSet usedSectors = new BitSet(Math.max((int) (file.size()/sectorSize), reservedSectors));
			for (int i = 0; i < reservedSectors; i++) {
				usedSectors.set(i, true);
			}
			// mark used sectors
			for (RegionEntryLocation loc : sectorMap) {
				if (sectorMap.isSpecial(loc)) {
					continue;
				}
				int offset = loc.getOffset();
				int size = loc.getSize();
				for (int i = 0; i < size; i++) {
					usedSectors.set(offset + i);
				}
			}
			return new SectorTracker<>(usedSectors, sectorMap);
		}
	}


}
