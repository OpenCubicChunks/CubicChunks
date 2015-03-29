/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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
package cubicchunks.util;

import cubicchunks.CubeCache;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;

public abstract class BlockColumnProcessor extends QueueProcessor {
	
	public BlockColumnProcessor(String name, CubeCache provider, int batchSize) {
		super(name, provider, batchSize);
	}
	
	@Override
	public void processBatch() {
		// start processing
		for (long address : this.incomingAddresses) {
			// get the block coords
			int blockX = Bits.unpackSigned(address, 26, 0);
			int blockZ = Bits.unpackSigned(address, 26, 26);
			
			// get the column
			int cubeX = Coords.blockToCube(blockX);
			int cubeZ = Coords.blockToCube(blockZ);
			Column column = (Column)this.cache.getChunk(cubeX, cubeZ);
			
			// skip blank columns
			if (column == null || column instanceof BlankColumn) {
				continue;
			}
			
			// get the local coords
			int localX = Coords.blockToLocal(blockX);
			int localZ = Coords.blockToLocal(blockZ);
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate(column, localX, localZ, blockX, blockZ);
			if (success) {
				this.processedAddresses.add(address);
			} else {
				this.deferredAddresses.add(address);
			}
		}
	}
	
	public abstract boolean calculate(Column column, int localX, int localZ, int blockX, int blockZ);
}
