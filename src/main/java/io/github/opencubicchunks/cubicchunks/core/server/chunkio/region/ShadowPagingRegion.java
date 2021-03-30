/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Optional;
import java.util.function.Function;

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
import cubicchunks.regionlib.util.WrappedException;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

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

	@Override public synchronized void writeValue(K key, ByteBuffer value) throws IOException {
		if (value == null) {
			this.sectorTracker.removeKey(key);
			updateHeaders(key);
			return;
		}
		int size = value.remaining();
		int sizeWithSizeInfo = size + Integer.BYTES;
		int numSectors = getSectorNumber(sizeWithSizeInfo);
		// this may throw UnsupportedDataException if data is too big
		RegionEntryLocation location = this.sectorTracker.reserveForKey(key, numSectors);

		int bytesOffset = location.getOffset()*sectorSize;

		Utils.writeFully(file.position(bytesOffset), ByteBuffer.allocate(Integer.BYTES).putInt(0, size));
		Utils.writeFully(file, value);
		file.force(false);
		updateHeaders(key);
		//no need to flush channel a second time after headers update, as doing so won't affect data integrity
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
		} catch (WrappedException e) {
			throw (IOException) e.get();
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
				throw new WrappedException(e);
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
		 * Returns offset for the given key and requestedSize, and reserves these sectors
		 */
		public RegionEntryLocation reserveForKey(K key, int requestedSize) throws IOException {
			Optional<RegionEntryLocation> existing = sectorMap.getEntryLocation(key);
			RegionEntryLocation found = findFree(requestedSize);
			this.sectorMap.setOffsetAndSize(key, found);
			this.updateUsedSectorsFor(existing.orElse(null), found);
			return found;
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
