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
package cubicchunks.generator.populator.generators;

import cubicchunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class WorldGenLakesCube extends WorldGeneratorCube {
	private final Block block;

	public WorldGenLakesCube(Block block) {
		this.block = block;
	}

	public boolean generate(World world, Random rand, int x, int y, int z) {
		int minY = getMinBlockYFromRandY(y);

		// find first non-empty block
		while (world.isAirBlock(x, y, z)) {
			if (--y < minY) {
				// don't generate if it isn't in our height range
				return false;
			}
		}

		// lake is always 16x8x16. Center is shifted by [-8, -4, -8]
		// moved from the beginning ot the method (to check for ai-block at the
		// center of lake)
		x -= 8;
		y -= 4;
		z -= 8;

		boolean[][][] setBlock = new boolean[16][8][16];
		int iterations = rand.nextInt(4) + 4;

		for (int i = 0; i < iterations; ++i) {
			double xScale = rand.nextDouble() * 6.0D + 3.0D;// <3;9)
			double yScale = rand.nextDouble() * 4.0D + 2.0D;// <2;6)
			double zScale = rand.nextDouble() * 6.0D + 3.0D;// <3;9)
			double xRadius = rand.nextDouble() * (16.0D - xScale - 2.0D) + 1.0D + xScale / 2.0D;// (6.5;15.5),
																								// lower
																								// are
																								// more
																								// probable
			double yRadius = rand.nextDouble() * (8.0D - yScale - 4.0D) + 2.0D + yScale / 2.0D;
			double zRadius = rand.nextDouble() * (16.0D - zScale - 2.0D) + 1.0D + zScale / 2.0D;// (6.5;15.5),
																								// lower
																								// are
																								// more
																								// probable

			for (int xRel = 1; xRel < 15; ++xRel) {
				for (int zRel = 1; zRel < 15; ++zRel) {
					for (int yRel = 1; yRel < 7; ++yRel) {
						double xDist = (xRel - xRadius) / (xScale / 2.0D);
						double yDist = (yRel - yRadius) / (yScale / 2.0D);
						double zDist = (zRel - zRadius) / (zScale / 2.0D);
						double distSquared = xDist * xDist + yDist * yDist + zDist * zDist;

						if (distSquared < 1.0D) {
							setBlock[xRel][yRel][zRel] = true;
						}
					}
				}
			}
		}

		for (int xRel = 0; xRel < 16; ++xRel) {
			for (int zRel = 0; zRel < 16; ++zRel) {
				for (int yRel = 0; yRel < 8; ++yRel) {
					boolean flag = !setBlock[xRel][yRel][zRel]
							&& (xRel < 15 && setBlock[xRel + 1][yRel][zRel] || xRel > 0
									&& setBlock[xRel - 1][yRel][zRel] || zRel < 15 && setBlock[xRel][yRel][zRel + 1]
									|| zRel > 0 && setBlock[xRel][yRel][zRel - 1] || yRel < 7
									&& setBlock[xRel][yRel + 1][zRel] || yRel > 0 && setBlock[xRel][yRel - 1][zRel]);

					if (flag) {
						Material material = world.getBlock(x + xRel, y + yRel, z + zRel).getMaterial();

						if (yRel >= 4 && material.isLiquid()) {
							return false;
						}

						if (yRel < 4 && !material.isSolid()
								&& world.getBlock(x + xRel, y + yRel, z + zRel) != this.block) {
							return false;
						}
					}
				}
			}
		}

		for (int xRel = 0; xRel < 16; ++xRel) {
			for (int zRel = 0; zRel < 16; ++zRel) {
				for (int yRel = 0; yRel < 8; ++yRel) {
					if (setBlock[xRel][yRel][zRel]) {
						world.setBlock(x + xRel, y + yRel, z + zRel, yRel >= 4 ? Blocks.air : this.block, 0, 2);
					}
				}
			}
		}

		for (int xRel = 0; xRel < 16; ++xRel) {
			for (int zRel = 0; zRel < 16; ++zRel) {
				for (int yRel = 4; yRel < 8; ++yRel) {
					if (setBlock[xRel][yRel][zRel] && world.getBlock(x + xRel, y + yRel - 1, z + zRel) == Blocks.dirt
							&& world.getSavedLightValue(EnumSkyBlock.Sky, x + xRel, y + yRel, z + zRel) > 0) {
						BiomeGenBase biome = world.getBiomeGenForCoords(x + xRel, z + zRel);

						if (biome.topBlock == Blocks.mycelium) {
							world.setBlock(x + xRel, y + yRel - 1, z + zRel, Blocks.mycelium, 0, 2);
						} else {
							world.setBlock(x + xRel, y + yRel - 1, z + zRel, Blocks.grass, 0, 2);
						}
					}
				}
			}
		}

		if (this.block.getMaterial() == Material.lava) {
			for (int xRel = 0; xRel < 16; ++xRel) {
				for (int zRel = 0; zRel < 16; ++zRel) {
					for (int yRel = 0; yRel < 8; ++yRel) {
						boolean flag = !setBlock[xRel][yRel][zRel]
								&& (xRel < 15 && setBlock[xRel + 1][yRel][zRel] || xRel > 0
										&& setBlock[xRel - 1][yRel][zRel] || zRel < 15
										&& setBlock[xRel][yRel][zRel + 1] || zRel > 0 && setBlock[xRel][yRel][zRel - 1]
										|| yRel < 7 && setBlock[xRel][yRel + 1][zRel] || yRel > 0
										&& setBlock[xRel][yRel - 1][zRel]);

						if (flag && (yRel < 4 || rand.nextInt(2) != 0)
								&& world.getBlock(x + xRel, y + yRel, z + zRel).getMaterial().isSolid()) {
							world.setBlock(x + xRel, y + yRel, z + zRel, Blocks.stone, 0, 2);
						}
					}
				}
			}
		}

		if (this.block.getMaterial() == Material.water) {
			for (int xRel = 0; xRel < 16; ++xRel) {
				for (int zRel = 0; zRel < 16; ++zRel) {
					if (world.isBlockFreezable(x + xRel, y + 4, z + zRel)) {
						world.setBlock(x + xRel, y + 4, z + zRel, Blocks.ice, 0, 2);
					}
				}
			}
		}

		return true;
	}
}
