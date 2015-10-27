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

import java.util.Set;

/*package-protected*/ class SkyLightUpdateProcessor extends QueueProcessor<SkyLightUpdateProcessor.Entry> {
	private final LightingManager lightingManager;

	SkyLightUpdateProcessor(LightingManager lightingManager, String name, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);
		this.lightingManager = lightingManager;
	}

	@Override
	public void processBatch(Progress progress) {
		SkyLightUpdateCalculator skylightUpdateCalculator = lightingManager.getSkylightUpdateCalculator();
		for(Entry entry : incomingAddresses) {
			Column column = cache.getColumn(entry.blockX >> 4, entry.blockZ >> 4);
			if(column == null || column instanceof BlankColumn) {
				continue;
			}
			int localX = Coords.blockToLocal(entry.blockX);
			int localZ = Coords.blockToLocal(entry.blockZ);
			Set<Integer> toDiffuse = skylightUpdateCalculator.calculate(column, localX, localZ, entry.minY, entry.startY);
			for(int y : toDiffuse) {
				SkyLightCubeDiffuseProcessor.Entry e = new SkyLightCubeDiffuseProcessor.Entry(entry.blockX, entry.blockZ, y);
				lightingManager.getSkylightCubeDiffuseProcessor().add(e);
			}
		}
		processedAddresses.addAll(incomingAddresses);
	}

	static final class Entry {
		private final int blockX, blockZ;
		private final int minY, startY;

		public Entry(int blockX, int blockZ, int minY, int startY) {
			this.blockX = blockX;
			this.blockZ = blockZ;
			this.minY = minY;
			this.startY = startY;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Entry entry = (Entry) o;

			if (blockX != entry.blockX) return false;
			if (blockZ != entry.blockZ) return false;
			if (minY != entry.minY) return false;
			return startY == entry.startY;

		}

		@Override
		public int hashCode() {
			int result = blockX;
			result = 31 * result + blockZ;
			result = 31 * result + minY;
			result = 31 * result + startY;
			return result;
		}
	}
}
