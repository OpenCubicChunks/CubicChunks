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
package cubicchunks.worldgen.generator.custom;

import cubicchunks.api.generators.ITerrainGenerator;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public final class TerrainProcessor extends CubeProcessor {
	private static final String PROCESSOR_NAME = "Terrain";

	private final ITerrainGenerator terrainGenerator;

	public TerrainProcessor(final ICubeCache cache, final int batchSize, final ITerrainGenerator terrainGen) {
		super(PROCESSOR_NAME, cache, batchSize);

		this.terrainGenerator = terrainGen;
	}

	@Override
	public boolean calculate(final Cube cube) {
		//cube.getWorld().profiler.startSection("terrainProcessor");
		
		//cube.getWorld().profiler.startSection("generation");
		double[][][] rawDensity = this.terrainGenerator.generate(cube);
		//cube.getWorld().profiler.endSection();
		
		generateTerrain(cube, rawDensity);
		
		//cube.getWorld().profiler.endSection();
		return true;
	}

	protected void generateTerrain(final Cube cube, final double[][][] densityField) {
		//cube.getWorld().profiler.startSection("placement");
		//todo: find better way to do it
		int seaLevel = cube.getWorld().provider.getAverageGroundLevel();
		for (int xRel = 0; xRel < 16; xRel++) {
			for (int zRel = 0; zRel < 16; zRel++) {
				for (int yRel = 0; yRel < 16; yRel++) {
					int yAbs = Coords.localToBlock(cube.getY(), yRel);
					BlockPos pos = new BlockPos(xRel, yRel, zRel);
					Block block = densityField[xRel][yRel][zRel] > 0 ? Blocks.STONE
							: yAbs < seaLevel ? Blocks.WATER : Blocks.AIR;
					cube.setBlockForGeneration(pos, block.getDefaultState());
				} // end yRel
			} // end zRel
		} // end xRel
		//cube.getWorld().profiler.endSection();
	}
}
