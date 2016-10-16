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

import cubicchunks.util.processor.QueueProcessor;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

class SkyLightCubeDiffuseProcessor extends QueueProcessor<Long> {

	private ICubicWorld world;

	SkyLightCubeDiffuseProcessor(ICubicWorld world, String name, int batchSize) {
		super(name, world.getCubeCache(), batchSize);
		this.world = world;
	}

	@Override
	public void processBatch() {
		for (Long address : incomingAddresses) {
			if (address == null) {
				throw new Error();
			}
			Cube cube = world.getCubeForAddress(address);
			if (cube != null)
				this.process(cube, address);
		}
	}

	private void process(Cube cube, long address) {
		int columnX = cube.getX();
		int columnZ = cube.getZ();
		int cubeY = cube.getY();

		Cube.LightUpdateData data = cube.getLightUpdateData();

		Column column = cache.provideChunk(columnX, columnZ);
		if (empty(column)) {
			return;
		}
		if (column.getLoadedCube(cubeY) == null) {
			return;
		}

		int done = 0;
		for (int i = 0; i < 256; i++) {
			int x = i >> 4;
			int z = i & 0xf;

			int minYLocal = data.getMin(x, z);
			int maxYLocal = data.getMax(x, z);

			if (minYLocal > maxYLocal) {
				done++;
				continue;
			}
			boolean b = SkyLightCubeDiffuseCalculator.calculate(column, x, z, cubeY, minYLocal, maxYLocal);
			if (b) {
				data.remove(x, z);
				done++;
			} else {
				//failed, don't waste more time here. Wait until we can do something here.
				break;
			}
		}
		(done == 256 ? processedAddresses : deferredAddresses).add(address);
	}

	private boolean empty(Column column) {
		return column == null || column instanceof BlankColumn;
	}
}
