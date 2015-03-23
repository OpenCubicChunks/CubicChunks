/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.util;

import cubicchunks.CubeCache;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;

public abstract class ColumnProcessor extends QueueProcessor {
	
	public ColumnProcessor(String name, CubeCache provider, int batchSize) {
		super(name, provider, batchSize);
	}
	
	@Override
	public void processBatch() {
		// start processing
		for (long address : m_incomingAddresses) {
			// get the column
			int cubeX = AddressTools.getX(address);
			int cubeZ = AddressTools.getZ(address);
			Column column = (Column)m_provider.provideChunk(cubeX, cubeZ);
			
			// skip blank columns
			if (column == null || column instanceof BlankColumn) {
				continue;
			}
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate(column);
			if (success) {
				m_processedAddresses.add(address);
			} else {
				m_deferredAddresses.add(address);
			}
		}
	}
	
	public abstract boolean calculate(Column column);
}
