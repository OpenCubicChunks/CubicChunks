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
package cubicchunks.generator;

import static cubicchunks.generator.terrain.GlobalGeneratorConfig.SEA_LEVEL;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.X_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.X_SECTION_SIZE;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Y_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Y_SECTION_SIZE;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Z_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Z_SECTION_SIZE;
import static cubicchunks.util.Coords.CUBE_MAX_X;
import static cubicchunks.util.Coords.CUBE_MAX_Y;
import static cubicchunks.util.Coords.CUBE_MAX_Z;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockPos;
import cubicchunks.generator.terrain.ITerrainGenerator;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;

public class TerrainProcessor extends CubeProcessor {
	private static final String PROCESSOR_NAME = "Terrain";
	
	public static double lerp(final double a, final double min, final double max) {
		return min + a * (max - min);
	}

	private final ITerrainGenerator terrainGenerator;
	
	protected double[][][] rawDensity;
	private final double[][][] finalDensity;

	public TerrainProcessor(final ICubeCache cache, final int batchSize, final ITerrainGenerator terrainGen) {
		super(PROCESSOR_NAME, cache, batchSize);
		
		this.terrainGenerator = terrainGen;

		this.rawDensity = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
		this.finalDensity = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
	}

	@Override
	public boolean calculate(final Cube cube) {
		
		this.rawDensity = this.terrainGenerator.generate(cube);
		
		expandNoiseArray();

		generateTerrain(cube);

		return true;
	}

	/**
	 * expand the noise array to 16x16x16 by interpolating the values.
	 *
	 * @param arrayIn
	 * @return
	 */
	public final void expandNoiseArray() {
		int xSteps = X_SECTION_SIZE - 1;
		int ySteps = Y_SECTION_SIZE - 1;
		int zSteps = Z_SECTION_SIZE - 1;

		// use the noise to generate the terrain
		for (int noiseX = 0; noiseX < X_SECTIONS - 1; noiseX++) {
			for (int noiseZ = 0; noiseZ < Z_SECTIONS - 1; noiseZ++) {
				for (int noiseY = 0; noiseY < Y_SECTIONS - 1; noiseY++) {
					// get the noise samples
					double x0y0z0 = this.rawDensity[noiseX][noiseY][noiseZ];
					double x0y0z1 = this.rawDensity[noiseX][noiseY][noiseZ + 1];
					double x1y0z0 = this.rawDensity[noiseX + 1][noiseY][noiseZ];
					double x1y0z1 = this.rawDensity[noiseX + 1][noiseY][noiseZ + 1];

					double x0y1z0 = this.rawDensity[noiseX][noiseY + 1][noiseZ];
					double x0y1z1 = this.rawDensity[noiseX][noiseY + 1][noiseZ + 1];
					double x1y1z0 = this.rawDensity[noiseX + 1][noiseY + 1][noiseZ];
					double x1y1z1 = this.rawDensity[noiseX + 1][noiseY + 1][noiseZ + 1];

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

								this.finalDensity[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}
	}

	public final void generateTerrain(final Cube cube) {
		for (int xRel = 0; xRel < 16; xRel++) {
			for (int zRel = 0; zRel < 16; zRel++) {
				for (int yRel = 0; yRel < 16; yRel++) {
					double val = this.finalDensity[xRel][yRel][zRel];

					int yAbs = Coords.localToBlock(cube.getY(), yRel);
					BlockPos pos = new BlockPos(xRel, yRel, zRel);
					Block block = val - yAbs > 0 ? Blocks.STONE : yAbs < SEA_LEVEL ? Blocks.WATER : Blocks.AIR;
					cube.setBlockForGeneration(pos, block.getDefaultState());
				} // end yRel
			} // end zRel
		} // end xRel
	}
}
