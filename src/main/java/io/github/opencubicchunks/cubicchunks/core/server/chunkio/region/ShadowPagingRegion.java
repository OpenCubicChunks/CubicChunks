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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
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

	/**
	 * An ordinary lock which prevents multiple writes from being started at once.
	 */
	private final Lock startWriteLock = new ReentrantLock();

	/**
	 * An {@link OngoingWrite} instance describing an in-progress write operation, or {@code null} if there is none.
	 */
	private volatile OngoingWrite<K> ongoingWrite = null;

	/**
	 * A read/write lock which allows writers to wait for all uncontended reads to complete.
	 */
	private final ReadWriteLock uncontendedReadLock = new ReentrantReadWriteLock(true);

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
		//calling file.force() is slow, so we want to minimize the number of times it needs to be called. the solution is simple: we write the data
		// for ALL entries at once, and don't update the headers until it's all been written to disk.

		//we assume that the given Map is safe for concurrent reads as long as we aren't writing to it (i legitimately have no idea what kind of
		// implementation *wouldn't* be - what datastructure changes its internal state during a read?)

		if (entries.isEmpty()) { //fast-path if there isn't anything to be written
			return;
		}

		//allocate temporary objects
		List<UnsupportedDataException.WithKey> exceptions = new ArrayList<>();
		Map<K, HeaderUpdate> pendingHeaderUpdates = new Object2ObjectOpenHashMap<>(entries.size());

		//actually perform the write
		this.doWrite(new OngoingWrite<>(Collections.emptyMap(), entries), ongoingWrite -> {
			try {
				//first pass: reserve header locations
				this.reserveHeaderEntriesPass(entries, exceptions, pendingHeaderUpdates);

				//second pass: write all data
				this.writeDataPass(entries, exceptions, pendingHeaderUpdates);
			} catch (RuntimeException | Error | IOException e) {
				//something went wrong, roll back all the pending updates
				pendingHeaderUpdates.forEach((key, update) -> {
					try {
						//roll back the update in the sector tracker
						this.sectorTracker.rollbackUpdate(key, update);
					} catch (RuntimeException | Error | IOException e1) {
						e.addSuppressed(e1);
					}
				});

				throw e;
			}

			try {
				//third pass: execute pending header updates
				this.doPendingHeaderUpdatesPass(pendingHeaderUpdates);
			} catch (RuntimeException | Error | IOException e) {
				//something went wrong, roll back all the pending updates
				pendingHeaderUpdates.forEach((key, update) -> {
					try {
						//roll back the update in the sector tracker
						this.sectorTracker.rollbackUpdate(key, update);
						//try to update the headers on-disk so that they're restored to their original value, as we don't want to have some
						// undefined subset of the writes be applied, but not all of them.
						this.updateHeaders(key);
					} catch (RuntimeException | Error | IOException e1) {
						e.addSuppressed(e1);
					}
				});

				throw e;
			}
		});

		//for all successfully written entries: advance the buffer's position to the end.
		// we couldn't do this while writing the data because it would have caused readers to return a clone of an empty buffer, rather than the
		// actual buffer range that was written.
		pendingHeaderUpdates.forEach((key, update) -> {
			ByteBuffer buffer = entries.get(key);
			buffer.position(buffer.limit());
		});

		//throw all pending exceptions at once if any occurred
		if (!exceptions.isEmpty()) {
			throw new MultiUnsupportedDataException(exceptions);
		}
	}

	private void reserveHeaderEntriesPass(Map<K, ByteBuffer> entries, List<UnsupportedDataException.WithKey> exceptions,
			Map<K, HeaderUpdate> pendingHeaderUpdates) throws IOException {
		for (Iterator<Map.Entry<K, ByteBuffer>> itr = entries.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry<K, ByteBuffer> entry = itr.next();

			K key = entry.getKey();
			ByteBuffer value = entry.getValue();

			HeaderUpdate update;
			if (value == null) {
				//if deleting an entry, there's no need to change anything on disk! the only thing that needs
				// to be changed is the headers.
				update = this.sectorTracker.getUpdateForDeletion(key);
			} else {
				int size = value.remaining();
				int sizeWithSizeInfo = size + Integer.BYTES;
				int numSectors = this.getSectorNumber(sizeWithSizeInfo);

				//this may throw UnsupportedDataException if data is too big.
				//it won't cause the sector tracker to be updated, meaning that reallocated sectors won't be overwritten by
				// subsequent writes from the same batch.
				try {
					update = this.sectorTracker.getUpdateWithReservation(key, numSectors);
				} catch (UnsupportedDataException e) {
					//save exception for later
					exceptions.add(new UnsupportedDataException.WithKey(e, key));
					continue;
				}
			}

			pendingHeaderUpdates.put(key, update);
		}
	}

	private void writeDataPass(Map<K, ByteBuffer> entries, List<UnsupportedDataException.WithKey> exceptions,
			Map<K, HeaderUpdate> pendingHeaderUpdates) throws IOException {
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
					//take a slice of the value buffer to prevent updating its position - the position must be unchanged so that concurrent readers
					// are able to clone the buffer. we'll update the positions of all the buffers after the write completes.
					value = value.slice();

					RegionEntryLocation location = pendingHeaderUpdates.get(key).getNext().get();

					int size = value.remaining();
					int bytesOffset = location.getOffset() * this.sectorSize;

					//build sequence of buffers for vectored IO
					tempBuffers.clear();
					tempBuffers.add(((ByteBuffer) lengthPrefixBuffer.clear()).putInt(0, size));
					tempBuffers.add(value);
					if ((lengthPrefixBuffer.capacity() + value.remaining()) % this.sectorSize != 0) { //pad trailing sector with zeroes
						zeroes(this.sectorSize - (lengthPrefixBuffer.capacity() + value.remaining()) % this.sectorSize, tempBuffers);
					}
					assert tempBuffers.stream().mapToInt(ByteBuffer::remaining).sum() == location.getSize() * this.sectorSize;

					//write all data to the entry's new location
					Utils.writeFully(this.file.position(bytesOffset), tempBuffers.toArray(new ByteBuffer[0]));

					//data has changed on disk, so we'll need to flush it before updating the headers
					shouldFlush = true;
				}
			} catch (UnsupportedDataException e) { //i'm about 99.9% sure that this can't be thrown - why is this code here?
				//save exception for later
				exceptions.add(new UnsupportedDataException.WithKey(e, key));
			}
		}

		//ensure that all file modifications are written to disk before we make any changes to the headers
		if (shouldFlush) {
			this.file.force(true);
		}
	}

	private void doPendingHeaderUpdatesPass(Map<K, HeaderUpdate> pendingHeaderUpdates) throws IOException {
		if (!pendingHeaderUpdates.isEmpty()) {
			//first, commit all the pending header updates to the index
			pendingHeaderUpdates.forEach(this.sectorTracker::commitUpdate);

			//update all the header slots on-disk
			for (K key : pendingHeaderUpdates.keySet()) {
				this.updateHeaders(key);
			}

			//ensure all header modifications are present on disk before another batch runs
			this.file.force(true);
		}
	}

	@Override public void writeSpecial(K key, Object marker) throws IOException {
		this.doWrite(new OngoingWrite<>(Collections.singletonMap(key, Optional.ofNullable(marker)), Collections.emptyMap()), ongoingWrite -> {
			this.sectorTracker.setSpecial(key, marker);
			this.updateHeaders(key);
			this.file.force(false);
		});
	}

	private void updateHeaders(K key) throws IOException {
		int entryByteCount = headerEntryProvider.getEntryByteCount();
		ByteBuffer buf = ByteBuffer.allocate(entryByteCount);
		headerEntryProvider.apply(key).write(buf);
		buf.flip();
		Utils.writeFully(file.position((long) key.getId() * entryByteCount), buf);
	}

	private void doWrite(OngoingWrite<K> ongoingWrite, CheckedConsumer<OngoingWrite<K>, ? extends IOException> writeBody) throws IOException {
		//we hold startWriteLock for the entire duration of the write to ensure that there's never more than one writer at once
		this.startWriteLock.lock();
		try {
			//set ongoingWrite, which will redirect all incoming readers to become contended reads on this write.
			assert this.ongoingWrite == null : "somehow, ongoingWrite returned!";
			this.ongoingWrite = ongoingWrite;

			try {
				//acquire uncontendedReadLock.writeLock(), which will cause us to block until all uncontended readers are completed. once it's been
				// acquired, we can be certain that any active readers are contended readers which are reading through our ongoingWrite instance, and
				// we can safely update to any keys which are present 'entries' map on disk without having to worry about concurrent readers.
				this.uncontendedReadLock.writeLock().lock();
				try {
					//now that all the locks have been acquired, we can actually perform the write
					writeBody.accept(ongoingWrite);
				} finally {
					//we've finished writing all the data we wanted to write, so we'll release uncontendedReadLock.writeLock() since readers should
					// now be able to safely access any entry on disk without us swapping the data out from underneath them.
					this.uncontendedReadLock.writeLock().unlock();
				}
			} finally {
				//reset ongoingWrite to null, which will prevent new readers from contending on this write.
				this.ongoingWrite = null;

				//block until all readers contending on this write have completed. we never release contendedReadLock.writeLock() - making sure it
				// remains locked permanently makes sure that any readers which somehow still hold a reference to it will be unable to start
				// contending on this write, and the lock will eventually be garbage collected.
				ongoingWrite.contendedReadLock.writeLock().lock();
			}
		} finally {
			//this write is complete! release startWriteLock so that the next writer can start.
			this.startWriteLock.unlock();
		}
	}

	@Override public Optional<ByteBuffer> readValue(K key) throws IOException {
		do {
			OngoingWrite<K> ongoingWrite = this.ongoingWrite;
			if (ongoingWrite != null) { //there is currently a write operation in progress
				if (!ongoingWrite.contendedReadLock.readLock().tryLock()) {
					//if we fail to acquire the write's contendedReadLock's read lock, it means the writer thread for this OngoingWrite has completed
					// and the OngoingWrite instance is outdated. spin and try again!
					continue;
				}
				try { //do a contended read, reading to-be-updated entries from memory and unmodified entries from disk
					if (ongoingWrite.inProgressWriteSpecial.containsKey(key)) {
						//the key is being modified by the ongoing write, so to avoid reading from disk (which will probably result in a data race)
						// we'll return the new value which is already in memory.

						//we actually can't do anything with the special value marker, as it has to be stored inside the sector map in order to
						// access the associated value. instead, we'll just spin until the write completes.
						Thread.yield();
						continue;
					} else if (ongoingWrite.inProgressWriteData.containsKey(key)) {
						//the key is being modified by the ongoing write, so to avoid reading from disk (which will probably result in a data race)
						// we'll return the new value which is already in memory.

						ByteBuffer originalWriteData = ongoingWrite.inProgressWriteData.get(key);
						if (originalWriteData == null) { //the entry is being deleted
							return Optional.empty();
						}

						//duplicate the buffer's contents to return them
						originalWriteData = originalWriteData.slice(); //slice the buffer first to avoid changing the position
						ByteBuffer clonedWriteData = ByteBuffer.allocate(originalWriteData.remaining());
						clonedWriteData.put(originalWriteData).clear();
						return Optional.of(clonedWriteData);
					} else {
						//the key is not being modified by the ongoing write. therefore, we can rest assured that nothing related to this key (either
						// its header entries or the data associated with it) will be modified during the course of the ongoing write, and can read
						// the data from disk like normal.
						//if a new write were to start before this read is complete, it's possible that this key could be modified by the new write
						// while this read is in progress. however, since the current write can't complete until it acquires
						// contendedReadLock.readLock(), and we hold contendedReadLock.readLock() until we finish reading, we can be certain that
						// a new write won't start modifying this key until we finish up here.
						return this.readFromDisk(key);
					}
				} finally {
					ongoingWrite.contendedReadLock.readLock().unlock();
				}
			}

			//there was no ongoing write, let's try to begin an uncontended read

			if (this.uncontendedReadLock.readLock().tryLock()) {
				//no writer is waiting to start, meaning that ongoingWrite is still null (if ongoingWrite is set while we hold the uncontendedRead
				// read lock it doesn't matter, because the writer won't be able to begin until all uncontended readers are finished)
				try {
					return this.readFromDisk(key);
				} finally {
					this.uncontendedReadLock.readLock().unlock();
				}
			}
		} while (true);
	}

	private Optional<ByteBuffer> readFromDisk(K key) throws IOException {
		Function<K, ByteBuffer> specialValue = this.sectorTracker.trySpecialValue(key).orElse(null);
		if (specialValue != null) {
			return Optional.of(specialValue.apply(key));
		}

		Optional<RegionEntryLocation> optionalLocation = this.sectorTracker.getEntryLocation(key);
		if (!optionalLocation.isPresent()) {
			return Optional.empty();
		}

		RegionEntryLocation location = optionalLocation.get();
		int sectorOffset = location.getOffset();
		int sectorCount = location.getSize();

		//read all the data in one go up to the end of the entry
		ByteBuffer buffer = ByteBuffer.allocate(sectorCount * this.sectorSize);
		readFully(this.file, buffer, sectorOffset * (long) this.sectorSize);
		buffer.flip();

		//read the actual data length
		int dataLength = buffer.getInt();
		if (dataLength > sectorCount * this.sectorSize) {
			throw new CorruptedDataException(
					"Expected data size max " + sectorCount * this.sectorSize + " but found " + dataLength);
		}

		//return a slice of the full buffer, so that the user doesn't get access to the length prefix or padding bytes at the end
		buffer.limit(buffer.position() + dataLength);
		return Optional.of(buffer.slice());
	}

	/**
	 * Returns true if something was stored there before within this region.
	 */
	@Override public boolean hasValue(K key) {
		do {
			OngoingWrite<K> ongoingWrite = this.ongoingWrite;
			if (ongoingWrite != null) { //there is currently a write operation in progress
				if (!ongoingWrite.contendedReadLock.readLock().tryLock()) {
					//if we fail to acquire the write's contendedReadLock's read lock, it means the writer thread for this OngoingWrite has completed
					// and the OngoingWrite instance is outdated. spin and try again!
					continue;
				}
				try { //do a contended read, reading to-be-updated entries from memory and unmodified entries from disk
					if (ongoingWrite.inProgressWriteSpecial.containsKey(key)) {
						//the key is being modified by the ongoing write, so to avoid reading from disk (which will probably result in a data race)
						// we'll return the new value which is already in memory.
						return true;
					} else if (ongoingWrite.inProgressWriteData.containsKey(key)) {
						//the key is being modified by the ongoing write, so to avoid reading from disk (which will probably result in a data race)
						// we'll return the new value which is already in memory.
						return ongoingWrite.inProgressWriteData.get(key) != null;
					} else {
						//the key is not being modified by the ongoing write. therefore, we can rest assured that nothing related to this key (either
						// its header entries or the data associated with it) will be modified during the course of the ongoing write, and can read
						// the data from disk like normal.
						return this.sectorTracker.trySpecialValue(key).isPresent() || this.sectorTracker.getEntryLocation(key).isPresent();
					}
				} finally {
					ongoingWrite.contendedReadLock.readLock().unlock();
				}
			}

			//there was no ongoing write, let's try to begin an uncontended read

			if (this.uncontendedReadLock.readLock().tryLock()) {
				//no writer is waiting to start, meaning that ongoingWrite is still null (if ongoingWrite is set while we hold the uncontendedRead
				// read lock it doesn't matter, because the writer won't be able to begin until all uncontended readers are finished)
				try {
					return this.sectorTracker.trySpecialValue(key).isPresent() || this.sectorTracker.getEntryLocation(key).isPresent();
				} finally {
					this.uncontendedReadLock.readLock().unlock();
				}
			}
		} while (true);
	}

	@Override public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
		//acquire write lock even when we are "only" reading because callbacks may write
		this.doWrite(new OngoingWrite<K>(Collections.emptyMap(), Collections.emptyMap()), ongoingWrite -> {
			final int keyCount = this.keyProvider.getKeyCount(this.regionKey);
			for (int id = 0; id < keyCount; id++) {
				int idFinal = id; // because java is stupid
				K key = this.sectorTracker.getEntryLocation(id).map(loc -> this.keyProvider.fromRegionAndId(this.regionKey, idFinal)).orElse(null);
				if (key != null) {
					cons.accept(key);
				}
			}
		});
	}

	private int getSectorNumber(int bytes) {
		return ceilDiv(bytes, sectorSize);
	}

	@Override
	public void flush() throws IOException {
		//CubicChunks.bigWarning(this + ": FLUSH!!!");
		this.doWrite(new OngoingWrite<K>(Collections.emptyMap(), Collections.emptyMap()), ongoingWrite -> {
			boolean fileLengthChanged = false;
			fileLengthChanged |= this.ensureSectorSizeAligned();

			//Flushable declares that flush() must ensure that "any buffered output" is written. IMO, erasing sectors (an action typically deferred
			//  until the region file is closed) can be considered buffered output, so we'll deal with erasing them here
			fileLengthChanged |= this.erasePendingSectors();

			//if the file's length changed, we want to make sure we also force metadata updates to disk
			this.file.force(fileLengthChanged);
		});
	}

	@Override public void close() throws IOException {
		//CubicChunks.bigWarning(this + ": CLOSE!!!");
		this.doWrite(new OngoingWrite<K>(Collections.emptyMap(), Collections.emptyMap()), ongoingWrite -> {
			//try-with-resources on file to ensure that the file gets closed, even if the other code throws an exception
			try (FileChannel file = this.file) {
				this.ensureSectorSizeAligned();
				this.erasePendingSectors();
			}
		});
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
			long read = src.read(data, position);
			if (read < 0L) {
				throw new EOFException();
			}
			position += read;
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

	private static class OngoingWrite<K extends IKey<K>> {
		/**
		 * A read/write lock which allows the writer to wait for all contended reads accessing this {@link OngoingWrite} instance to complete.
		 * <p>
		 * Once complete, the writer will acquire this lock's {@link ReadWriteLock#writeLock() write lock} and never release it, thus preventing any
		 * new readers from becoming contended writes on this {@link OngoingWrite} instance and informing them that this write is complete.
		 */
		public final ReadWriteLock contendedReadLock = new ReentrantReadWriteLock(true);

		/**
		 * An immutable map representing a view of the changes being made by this write operation.
		 */
		public final Map<K, Optional<Object>> inProgressWriteSpecial;

		/**
		 * An immutable map representing a view of the changes being made by this write operation.
		 */
		public final Map<K, ByteBuffer> inProgressWriteData;

		public OngoingWrite(Map<K, Optional<Object>> inProgressWriteSpecial, Map<K, ByteBuffer> inProgressWriteData) {
			this.inProgressWriteSpecial = inProgressWriteSpecial;
			this.inProgressWriteData = inProgressWriteData;
		}
	}

	private static class HeaderUpdate {
		private final RegionEntryLocation prev;
		private final RegionEntryLocation next;

		@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
		public HeaderUpdate(Optional<RegionEntryLocation> prev, Optional<RegionEntryLocation> next) {
			this.prev = prev.orElse(null);
			this.next = next.orElse(null);
		}

		public Optional<RegionEntryLocation> getPrev() {
			return Optional.ofNullable(this.prev);
		}

		public Optional<RegionEntryLocation> getNext() {
			return Optional.ofNullable(this.next);
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
		 * Reserves {@code requestedSize} new sectors, marks them as used and updates the entry location for the given {@code key}. If a valid
		 * location cannot be found, {@link UnsupportedOperationException} and this sector tracker will not be modified.
		 * <p>
		 * The old sectors will not be released.
		 *
		 * @throws UnsupportedDataException if the sector map cannot store a value of the requested size
		 */
		public HeaderUpdate getUpdateWithReservation(K key, int requestedSize) throws IOException, UnsupportedDataException {
			Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);
			RegionEntryLocation found = this.findFree(requestedSize);

			this.sectorMap.setOffsetAndSize(key, found); //this will throw UnsupportedDataException without changing anything if it fails
			this.updateUsedSectorsFor(null, found); //mark new sectors as allocated

			return new HeaderUpdate(existing, Optional.of(found));
		}

		/**
		 * Prepares a {@link HeaderUpdate} for deleting the entry with the given {@code key}.
		 * <p>
		 * The old sectors will not be released.
		 *
		 * @throws UnsupportedDataException if the sector map cannot store a value of the requested size
		 */
		public HeaderUpdate getUpdateForDeletion(K key) throws IOException, UnsupportedDataException {
			Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);

			this.sectorMap.setOffsetAndSize(key, new RegionEntryLocation(0, 0));

			//we don't want to de-allocate the old sectors yet, so don't call updateUsedSectorsFor

			return new HeaderUpdate(existing, Optional.empty());
		}

		public void commitUpdate(K key, HeaderUpdate update) {
			//the new entry location should already be stored in the sector map, so we don't have to make any changes to it here.

			//release the previously occupied sectors (we assume that the new sectors are already marked as used)
			this.updateUsedSectorsFor(update.getPrev().orElse(null), null);
		}

		public void rollbackUpdate(K key, HeaderUpdate update) throws IOException {
			//restore original state in the sector map
			this.sectorMap.setOffsetAndSize(key, update.getPrev().orElseGet(() -> new RegionEntryLocation(0, 0)));

			//free all the newly allocated sectors and re-mark the previously used sectors as used
			this.updateUsedSectorsFor(update.getNext().orElse(null), update.getPrev().orElse(null));
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
