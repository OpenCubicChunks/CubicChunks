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
package cubicchunks.util;

import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import static cubicchunks.worldgen.generator.GlobalGeneratorConfig.*;
import static cubicchunks.util.Coords.CUBE_SIZE;
import static cubicchunks.util.MathUtil.lerp;

public final class TerrainGeneratorUtils {
	/**
	 * expand the noise array to 16x16x16 by interpolating the values.
	 */
	public static double[][][] expandNoiseArray(double[][][] input, double[][][] output) {
		int xSteps = X_SECTION_SIZE - 1;
		int ySteps = Y_SECTION_SIZE - 1;
		int zSteps = Z_SECTION_SIZE - 1;

		// use the noise to generate the generator
		for (int noiseX = 0; noiseX < X_SECTIONS - 1; noiseX++) {
			for (int noiseZ = 0; noiseZ < Z_SECTIONS - 1; noiseZ++) {
				for (int noiseY = 0; noiseY < Y_SECTIONS - 1; noiseY++) {
					// get the noise samples
					double x0y0z0 = input[noiseX][noiseY][noiseZ];
					double x0y0z1 = input[noiseX][noiseY][noiseZ + 1];
					double x1y0z0 = input[noiseX + 1][noiseY][noiseZ];
					double x1y0z1 = input[noiseX + 1][noiseY][noiseZ + 1];

					double x0y1z0 = input[noiseX][noiseY + 1][noiseZ];
					double x0y1z1 = input[noiseX][noiseY + 1][noiseZ + 1];
					double x1y1z0 = input[noiseX + 1][noiseY + 1][noiseZ];
					double x1y1z1 = input[noiseX + 1][noiseY + 1][noiseZ + 1];

					for (int x = 0; x < xSteps; x++) {
						int xRel = noiseX * xSteps + x;

						double xd = (double) x / xSteps;

						// interpolate along x
						double xy0z0 = lerp(xd, x0y0z0, x1y0z0);
						double xy0z1 = lerp(xd, x0y0z1, x1y0z1);
						double xy1z0 = lerp(xd, x0y1z0, x1y1z0);
						double xy1z1 = lerp(xd, x0y1z1, x1y1z1);

						for (int z = 0; z < zSteps; z++) {
							int zRel = noiseZ * zSteps + z;

							double zd = (double) z / zSteps;

							// interpolate along z
							double xy0z = lerp(zd, xy0z0, xy0z1);
							double xy1z = lerp(zd, xy1z0, xy1z1);

							for (int y = 0; y < ySteps; y++) {
								int yRel = noiseY * ySteps + y;

								double yd = (double) y / ySteps;

								// interpolate along y
								double xyz = lerp(yd, xy0z, xy1z);

								output[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}

		return output;
	}

	public static double[][][] getNewCubeSizedArray() {
		return new double[CUBE_SIZE][CUBE_SIZE][CUBE_SIZE];
	}

	public static void applyHeightGradient(final Cube cube, final double[][][] rawDensity) {
		final int cubeYMin = Coords.cubeToMinBlock(cube.getY());
		
		for (int x = 0; x < CUBE_SIZE; x++) {
			for (int z = 0; z < CUBE_SIZE; z++) {
				for (int y = 0; y < CUBE_SIZE; y++) {
					final int yAbs = cubeYMin + y;
					
					rawDensity[x][y][z] -= yAbs;
				}
			}
		}
	}

	public static void generateTerrain(final Cube cube, final double[][][] densityField) {
		//cube.getWorld().profiler.startSection("placement");
		//todo: find better way to do it
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int seaLevel = cube.getWorld().provider.getAverageGroundLevel();
		for (int xRel = 0; xRel < 16; xRel++) {
			for (int zRel = 0; zRel < 16; zRel++) {
				for (int yRel = 0; yRel < 16; yRel++) {
					int yAbs = Coords.localToBlock(cube.getY(), yRel);
					pos.set(xRel, yRel, zRel);
					Block block = densityField[xRel][yRel][zRel] > 0 ? Blocks.STONE
							: yAbs < seaLevel ? Blocks.WATER : Blocks.AIR;
					cube.setBlockForGeneration(pos, block.getDefaultState());
				} // end yRel
			} // end zRel
		} // end xRel
		//cube.getWorld().profiler.endSection();
	}
}
