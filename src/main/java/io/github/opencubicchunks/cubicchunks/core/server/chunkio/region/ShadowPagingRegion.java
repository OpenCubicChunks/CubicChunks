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
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.*;

/**
 * Simplified {@link Region} class implementing shadow paging by using custom sector tracker
 * that doesn't reallocate entries in place.
 */
public class ShadowPagingRegion<K extends IKey<K>> implements IRegion<K> {

	private final FileChannel file;
	private final IHeaderDataEntryProvider<?, K> headerEntryProvider;
	private final RegionKey regionKey;
	private final IKeyProvider<K> keyProvider;
	private final int sectorSize;
	private final SectorTracker<K> sectorTracker;

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
	public synchronized void writeValues(Map<K, ByteBuffer> entries) throws IOException {
		if (entries.isEmpty()) { //fast-path if there isn't anything to be written
			return;
		}

		//calling file.force() is slow, so we want to minimize the number of times it needs to be called. the solution is simple: we write the data for ALL
		//  entries at once, and don't update the headers until it's all been written to disk.
		List<UnsupportedDataException.WithKey> exceptions = new ArrayList<>();
		Map<K, Optional<RegionEntryLocation>> pendingHeaderUpdates = new HashMap<>(entries.size());

		//first pass: write all data
		boolean shouldFlush = false;
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

					int bytesOffset = headerUpdate.getSecond().getOffset() * this.sectorSize;
					Utils.writeFully(this.file.position(bytesOffset), ByteBuffer.allocate(Integer.BYTES).putInt(0, size));
					Utils.writeFully(this.file, value);

					//data has changed on disk, so we'll need to flush it before updating the headers
					shouldFlush = true;
				}
				pendingHeaderUpdates.put(key, prevLocation);
			} catch (UnsupportedDataException e) {
				//save exception for later
				exceptions.add(new UnsupportedDataException.WithKey(e, key));
			}
		}

		//flush the file's contents if any of the entries modified the region data
		if (shouldFlush) {
			this.file.force(true);
		}

		//second pass: execute pending header updates
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

		//throw all pending exceptions at once if any occurred
		if (!exceptions.isEmpty()) {
			throw new MultiUnsupportedDataException(exceptions);
		}
	}

	@Override public void writeSpecial(K key, Object marker) throws IOException {
		this.sectorTracker.setSpecial(key, marker);
		updateHeaders(key);
		file.force(false);
	}

	private void updateHeaders(K key) throws IOException {
		int entryByteCount = headerEntryProvider.getEntryByteCount();
		ByteBuffer buf = ByteBuffer.allocate(entryByteCount);
		headerEntryProvider.apply(key).write(buf);
		buf.flip();
		Utils.writeFully(file.position((long) key.getId() * entryByteCount), buf);
	}

	@Override public synchronized Optional<ByteBuffer> readValue(K key) throws IOException {
		// a hack because Optional can't throw checked exceptions
		try {
			return sectorTracker.trySpecialValue(key)
					.map(reader -> Optional.of(reader.apply(key)))
					.orElseGet(() -> doReadKey(key));
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private Optional<ByteBuffer> doReadKey(K key) {
		return sectorTracker.getEntryLocation(key).flatMap(loc -> {
			try {
				int sectorOffset = loc.getOffset();
				int sectorCount = loc.getSize();

				ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);

				Utils.readFully(file.position((long) sectorOffset * sectorSize), buf);

				int dataLength = buf.getInt(0);
				if (dataLength > sectorCount * sectorSize) {
					throw new CorruptedDataException(
							"Expected data size max" + sectorCount * sectorSize + " but found " + dataLength);
				}

				ByteBuffer bytes = ByteBuffer.allocate(dataLength);
				Utils.readFully(file, bytes);
				bytes.flip();
				return Optional.of(bytes);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Returns true if something was stored there before within this region.
	 */
	@Override public synchronized boolean hasValue(K key) {
		return sectorTracker.getEntryLocation(key).isPresent();
	}

	@Override public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
		int keyCount = this.keyProvider.getKeyCount(regionKey);
		for (int id = 0; id < keyCount; id++) {
			int idFinal = id; // because java is stupid
			K key = sectorTracker.getEntryLocation(id).map(loc -> keyProvider.fromRegionAndId(this.regionKey, idFinal)).orElse(null);
			if (key != null) {
				cons.accept(key);
			}
		}
	}


	private int getSectorNumber(int bytes) {
		return ceilDiv(bytes, sectorSize);
	}

	@Override
	public void flush() throws IOException {
		this.ensureSectorSizeAligned();
		this.file.force(false);
	}

	@Override public void close() throws IOException {
		this.ensureSectorSizeAligned();
		this.file.close();
	}

	private void ensureSectorSizeAligned() throws IOException {
		if (file.size() % sectorSize != 0) {
			int extra = (int) (sectorSize - (file.size() % sectorSize));
			ByteBuffer buffer = ByteBuffer.allocateDirect(extra);
			this.file.position(this.file.size());
			Utils.writeFully(this.file, buffer);
			assert this.file.size() % sectorSize == 0;
		}
	}

	private static int ceilDiv(int x, int y) {
		return -Math.floorDiv(-x, y);
	}

	public static <L extends IKey<L>> ShadowPagingRegion.Builder<L> builder() {
		return new ShadowPagingRegion.Builder<>();
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
			}
			if (newSectorLocation != null) {
				int newOffset = newSectorLocation.getOffset();
				usedSectors.set(newOffset, newOffset + newSectorLocation.getSize(), true);
			}
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
