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
package cubicchunks.generator.terrain;

import static cubicchunks.generator.terrain.GlobalGeneratorConfig.*;
import static cubicchunks.util.Coords.CUBE_MAX_X;
import static cubicchunks.util.Coords.CUBE_MAX_Y;
import static cubicchunks.util.Coords.CUBE_MAX_Z;
import cubicchunks.generator.builder.IBuilder;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeProcessor;
import cubicchunks.world.Cube;
import cubicchunks.world.CubeCache;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractTerrainProcessor3dNoise extends CubeProcessor {

	protected final double[][][] noiseArrayHigh;
	protected final double[][][] noiseArrayLow;
	protected final double[][][] noiseArrayAlpha;

	protected final double[][][] rawTerrainArray;
	protected final double[][][] terrainArray;

	protected final IBuilder builderHigh;
	protected final IBuilder builderLow;
	protected final IBuilder builderAlpha;

	protected final int seaLevel;

	protected boolean amplify;

	protected final World worldServer;

	public AbstractTerrainProcessor3dNoise(String name, World worldServer, CubeCache cache, int batchSize) {
		super(name, cache, batchSize);

		this.worldServer = worldServer;

		this.noiseArrayHigh = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];
		this.noiseArrayLow = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];
		this.noiseArrayAlpha = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];

		this.rawTerrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
		this.terrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];

		this.builderHigh = createHighBuilder();
		this.builderLow = createLowBuilder();
		this.builderAlpha = createAlphaBuilder();

		this.amplify = true;
		this.seaLevel = 0;// worldServer.getCubeWorldProvider().getSeaLevel();
	}

	protected abstract IBuilder createHighBuilder();

	protected abstract IBuilder createLowBuilder();

	protected abstract IBuilder createAlphaBuilder();

	@Override
	public boolean calculate(Cube cube) {
		this.generateNoiseArrays(cube);

		this.generateTerrainArray(cube);

		if (amplify) {
			this.amplifyNoiseArray();
		}
		this.expandNoiseArray();

		this.generateTerrain(cube);

		return true;
	}

	/**
	 * Generates noise arrays of size X_NOISE_SIZE * Y_NOISE_SIZE * Z_NOISE_SIZE
	 * 
	 * No issues with this. Tested by using size 16 (full resolution)
	 * 
	 * @param cubeX
	 * @param cubeY
	 * @param cubeZ
	 * @return
	 */
	private void generateNoiseArrays(Cube cube) {
		int cubeXMin = cube.getX() * (X_SECTIONS - 1);
		int cubeYMin = cube.getY() * (Y_SECTIONS - 1);
		int cubeZMin = cube.getZ() * (Z_SECTIONS - 1);

		for (int x = 0; x < X_SECTIONS; x++) {
			int xPos = cubeXMin + x;

			for (int z = 0; z < Z_SECTIONS; z++) {
				int zPos = cubeZMin + z;

				for (int y = 0; y < Y_SECTIONS; y++) {
					int yPos = cubeYMin + y;

					this.noiseArrayHigh[x][y][z] = builderHigh.getValue(xPos, yPos, zPos);
					this.noiseArrayLow[x][y][z] = builderLow.getValue(xPos, yPos, zPos);
					this.noiseArrayAlpha[x][y][z] = builderAlpha.getValue(xPos, yPos, zPos);

				}
			}
		}
	}

	protected abstract void generateTerrainArray(Cube cube);

	private void amplifyNoiseArray() {
		for (int x = 0; x < X_SECTIONS; x++) {
			for (int z = 0; z < Z_SECTIONS; z++) {
				for (int y = 0; y < Y_SECTIONS; y++) {
					this.rawTerrainArray[x][y][z] *= maxElev;
				}
			}
		}
	}

	/**
	 * expand the noise array to 16x16x16 by interpolating the values.
	 * 
	 * @param arrayIn
	 * @return
	 */
	private void expandNoiseArray() {
		int xSteps = X_SECTION_SIZE - 1;
		int ySteps = Y_SECTION_SIZE - 1;
		int zSteps = Z_SECTION_SIZE - 1;

		// use the noise to generate the terrain
		for (int noiseX = 0; noiseX < X_SECTIONS - 1; noiseX++) {
			for (int noiseZ = 0; noiseZ < Z_SECTIONS - 1; noiseZ++) {
				for (int noiseY = 0; noiseY < Y_SECTIONS - 1; noiseY++) {
					// get the noise samples
					double x0y0z0 = rawTerrainArray[noiseX][noiseY][noiseZ];
					double x0y0z1 = rawTerrainArray[noiseX][noiseY][noiseZ + 1];
					double x1y0z0 = rawTerrainArray[noiseX + 1][noiseY][noiseZ];
					double x1y0z1 = rawTerrainArray[noiseX + 1][noiseY][noiseZ + 1];

					double x0y1z0 = rawTerrainArray[noiseX][noiseY + 1][noiseZ];
					double x0y1z1 = rawTerrainArray[noiseX][noiseY + 1][noiseZ + 1];
					double x1y1z0 = rawTerrainArray[noiseX + 1][noiseY + 1][noiseZ];
					double x1y1z1 = rawTerrainArray[noiseX + 1][noiseY + 1][noiseZ + 1];

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

								terrainArray[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}
	}

	protected double lerp(double a, double min, double max) {
		return min + a * (max - min);
	}

	protected void generateTerrain(Cube cube) {
		int cubeY = cube.getY();

		for (int xRel = 0; xRel < 16; xRel++) {
			for (int zRel = 0; zRel < 16; zRel++) {
				for (int yRel = 0; yRel < 16; yRel++) {
					double val = terrainArray[xRel][yRel][zRel];

					int yAbs = Coords.localToBlock(cubeY, yRel);
					BlockPos pos = new BlockPos(xRel, yRel, zRel);
					Block block = val - yAbs > 0 ? Blocks.STONE
							: yAbs < seaLevel ? Blocks.WATER : Blocks.AIR;
					cube.setBlockForGeneration(pos, block.getDefaultState());
				} // end yRel
			} // end zRel
		} // end xRel
	}
}
