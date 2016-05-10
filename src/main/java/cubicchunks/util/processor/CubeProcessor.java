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
package cubicchunks.util.processor;

import cubicchunks.util.AddressTools;
import cubicchunks.util.Progress;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;

import java.util.Set;

public abstract class CubeProcessor extends QueueProcessor<Long> {

	public CubeProcessor(String name, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);
	}

	@Override
	public void processBatch(Progress progress) {
		// start processing
		for (long address : this.incomingAddresses) {
			if (processedAddresses.contains(address)) {
				//this address has been processed additionally
				continue;
			}

			// get the cube
			int cubeX = AddressTools.getX(address);
			int cubeY = AddressTools.getY(address);
			int cubeZ = AddressTools.getZ(address);
			Cube cube = this.cache.getCube(cubeX, cubeY, cubeZ);
			if (cube == null) {
				// this cube probably got unloaded before it could be processed
				// just drop it from the queue
				continue;
			}

			// add unsuccessful calculations back onto the queue
			Set<Cube> generatedCubes = calculate(cube);
			if (!generatedCubes.contains(cube)) {
				this.deferredAddresses.add(address);
			}
			for (Cube c : generatedCubes) {
				this.processedAddresses.add(c.getAddress());
			}

			if (progress != null) {
				progress.incrementProgress();
			}
		}
	}

	public abstract Set<Cube> calculate(Cube cube);
}
