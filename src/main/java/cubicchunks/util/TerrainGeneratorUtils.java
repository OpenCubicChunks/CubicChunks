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

import static cubicchunks.generator.terrain.GlobalGeneratorConfig.X_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.X_SECTION_SIZE;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Y_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Y_SECTION_SIZE;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Z_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Z_SECTION_SIZE;
import static cubicchunks.util.Coords.CUBE_SIZE;
import static cubicchunks.util.MathHelper.lerp;
import cubicchunks.world.cube.Cube;

public final class TerrainGeneratorUtils {
	/**
	 * expand the noise array to 16x16x16 by interpolating the values.
	 */
	public static double[][][] expandNoiseArray(final double[][][] input) {
		double[][][] result = getNewCubeSizedArray();
		int xSteps = X_SECTION_SIZE - 1;
		int ySteps = Y_SECTION_SIZE - 1;
		int zSteps = Z_SECTION_SIZE - 1;

		// use the noise to generate the terrain
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

								result[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}

		return result;
	}

	public static double[][][] getNewCubeSizedArray() {
		return new double[CUBE_SIZE][CUBE_SIZE][CUBE_SIZE];
	}

	public static double[][][] applyHeightGradient(final Cube cube, final double[][][] rawDensity) {
		final double [][][] result = getNewCubeSizedArray();
		
		final int cubeYMin = Coords.cubeToMinBlock(cube.getY());
		
		for (int x = 0; x < CUBE_SIZE; x++) {
			for (int z = 0; z < CUBE_SIZE; z++) {
				for (int y = 0; y < CUBE_SIZE; y++) {
					final int yAbs = cubeYMin + y;
					
					result[x][y][z] = rawDensity[x][y][z] - yAbs;
				}
			}
		}
		
		return result;
	}
}
