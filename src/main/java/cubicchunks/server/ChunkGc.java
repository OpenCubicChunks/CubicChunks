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
package cubicchunks.server;

import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import java.util.Iterator;

/**
 * Chunk Garbage Collector, automatically unloads unused chunks.
 */
public class ChunkGc {
	// GC every 10 seconds by default
	private static final int GC_INTERVAL = 20*10;

	private final CubeProviderServer cubeCache;

	private int tick = 0;

	public ChunkGc(CubeProviderServer cubeCache) {
		this.cubeCache = cubeCache;
	}

	public void tick() {
		tick++;
		if (tick > GC_INTERVAL) {
			tick = 0;
			chunkGc();
		}
	}

	private void chunkGc() {
		Iterator<Cube> cubeIt = cubeCache.cubesIterator();
		while(cubeIt.hasNext()) {
			if(cubeCache.tryUnloadCube(cubeIt.next())) {
				cubeIt.remove();
			}
		}

		Iterator<Column> columnIt = cubeCache.columnsIterator();
		while(columnIt.hasNext()) {
			if(cubeCache.tryUnloadColumn(columnIt.next())) {
				columnIt.remove();
			}
		}
	}
}
