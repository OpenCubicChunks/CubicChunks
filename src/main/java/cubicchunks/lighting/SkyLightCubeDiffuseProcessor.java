/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.lighting;

import cubicchunks.util.Coords;
import cubicchunks.util.Progress;
import cubicchunks.util.processor.QueueProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;

class SkyLightCubeDiffuseProcessor extends QueueProcessor<SkyLightCubeDiffuseProcessor.Entry> {


	private LightingManager lightingManager;

	public SkyLightCubeDiffuseProcessor(LightingManager lightingManager, String name, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);
		this.lightingManager = lightingManager;
	}

	@Override
	public void processBatch(Progress progress) {
		SkyLightCubeDiffuseCalculator skylightCubeDiffuseCalculator = lightingManager.getSkylightCubeDiffuseCalculator();
		for (Entry e : incomingAddresses) {
			int columnX = Coords.blockToCube(e.blockX);
			int columnZ = Coords.blockToCube(e.blockZ);

			Column column = cache.getColumn(columnX, columnZ);
			if(empty(column)) {
				continue;
			}
			if(column.getCube(e.cubeY) == null) {
				continue;
			}
			if (empty(cache.getColumn(columnX + 1, columnZ)) ||
					empty(cache.getColumn(columnX - 1, columnZ)) ||
					empty(cache.getColumn(columnX, columnZ + 1)) ||
					empty(cache.getColumn(columnX, columnZ - 1)) ||
					empty(cache.getColumn(columnX + 1, columnZ + 1)) ||
					empty(cache.getColumn(columnX + 1, columnZ - 1)) ||
					empty(cache.getColumn(columnX - 1, columnZ + 1)) ||
					empty(cache.getColumn(columnX - 1, columnZ - 1))) {
				deferredAddresses.add(e);
				continue;
			}


			int localX = Coords.blockToLocal(e.blockX);
			int localZ = Coords.blockToLocal(e.blockZ);

			boolean updated = skylightCubeDiffuseCalculator.calculate(column, localX, localZ, e.cubeY);
			(updated ? processedAddresses : deferredAddresses).add(e);
		}
	}

	private boolean empty(Column column) {
		return column == null || column instanceof BlankColumn;
	}

	public static class Entry {
		private final int blockX, blockZ, cubeY;

		public Entry(int blockX, int blockZ, int cubeY) {
			this.blockX = blockX;
			this.blockZ = blockZ;
			this.cubeY = cubeY;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Entry entry = (Entry) o;

			if (blockX != entry.blockX) return false;
			if (blockZ != entry.blockZ) return false;
			return cubeY == entry.cubeY;

		}

		@Override
		public int hashCode() {
			int result = blockX;
			result = 31 * result + blockZ;
			result = 31 * result + cubeY;
			return result;
		}
	}
}
