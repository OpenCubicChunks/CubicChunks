/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

public abstract class CubeProcessor extends QueueProcessor {
	
	public CubeProcessor(String name, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);
	}
	
	@Override
	public void processBatch(Progress progress) {
		
		// start processing
		for (long address : this.incomingAddresses) {
			
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
			boolean success = calculate(cube);
			if (success) {
				this.processedAddresses.add(address);
			} else {
				this.deferredAddresses.add(address);
			}
			
			if (progress != null) {
				progress.incrementProgress();
			}
		}
	}
	
	public abstract boolean calculate(Cube cube);
}
