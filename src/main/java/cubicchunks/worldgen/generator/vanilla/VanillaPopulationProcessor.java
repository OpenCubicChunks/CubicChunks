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
package cubicchunks.worldgen.generator.vanilla;

import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.CubeCoords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.gen.ChunkProviderOverworld;

public class VanillaPopulationProcessor implements CubeProcessor {
	private final ChunkProviderOverworld vanillaGen;

	public VanillaPopulationProcessor(ChunkProviderOverworld vanillaGen) {
		this.vanillaGen = vanillaGen;
	}

	@Override
	public void calculate(Cube cube) {
		if(cube.getY() < 0 || cube.getY() >= 16) {
			return;
		}
		if(!cube.getColumn().isCompatPopulationDone()) {
			cube.getColumn().setCompatPopulationDone(true);
			this.populate(cube);
		}
	}

	private void populate(Cube cube) {
		Column column = cube.getColumn();
		for(int y = 0; y < 16; y++) {
			if(column.getCube(y) == null || column.getCube(y).unloaded) {
				//doing that here should avoid population of the same chunk while loading/generating needed cubes
				//this shouldn't really generate anything, but it may happen when world save is incomplete
				((ServerCubeCache)cube.getWorld().getCubeCache()).loadCube(new CubeCoords(cube.getX(), y, cube.getZ()), ServerCubeCache.LoadType.LOAD_OR_GENERATE);
			}
		}
		try {
			this.vanillaGen.populate(cube.getX(), cube.getZ());
		} catch (RuntimeException ex) {
			CubicChunks.LOGGER.error("Exception when populating chunk at {}", cube.getCoords(), ex);
		}
	}
}
