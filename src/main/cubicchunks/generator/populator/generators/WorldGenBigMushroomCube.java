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

public class WorldGenBigMushroomCube extends WorldGeneratorCube {
	public static final int BROWN = 0, RED = 1, UNSPECIFIED = -1;

	private final int mushroomType;

	public WorldGenBigMushroomCube(int type) {
		super(true);
		this.mushroomType = type;
	}

	public WorldGenBigMushroomCube() {
		super(false);
		this.mushroomType = UNSPECIFIED;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int genType = rand.nextInt(2);

		if (this.mushroomType != UNSPECIFIED) {
			genType = this.mushroomType;
		}

		int height = rand.nextInt(3) + 4;
		boolean canGenerate = true;

		for (int blockY = y; blockY <= y + 1 + height; ++blockY) {
			byte radius = 3;

			if (blockY <= y + 3) {
				radius = 0;
			}

			for (int blockX = x - radius; blockX <= x + radius && canGenerate; ++blockX) {
				for (int blockZ = z - radius; blockZ <= z + radius && canGenerate; ++blockZ) {
					Block block = world.getBlock(blockX, blockY, blockZ);

					if (block.getMaterial() != Material.air && block.getMaterial() != Material.leaves) {
						canGenerate = false;
					}
				}
			}
		}

		if (!canGenerate) {
			return false;
		}

		Block block = world.getBlock(x, y - 1, z);

		if (block != Blocks.dirt && block != Blocks.grass && block != Blocks.mycelium) {
			return false;
		}
		int startY = y + height;

		if (genType == RED) {
			startY = y + height - 3;
		}

		// Generate top of the mushroom
		for (int blockY = startY; blockY <= y + height; ++blockY) {
			int radius = 1;

			if (blockY < y + height) {
				++radius;
			}

			if (genType == BROWN) {
				radius = 3;
			}

			for (int blockX = x - radius; blockX <= x + radius; ++blockX) {
				for (int blockZ = z - radius; blockZ <= z + radius; ++blockZ) {
					int rotation = 5;

					if (blockX == x - radius) {
						--rotation;
					}

					if (blockX == x + radius) {
						++rotation;
					}

					if (blockZ == z - radius) {
						rotation -= 3;
					}

					if (blockZ == z + radius) {
						rotation += 3;
					}

					if (genType == BROWN || blockY < y + height) {
						if ((blockX == x - radius || blockX == x + radius)
								&& (blockZ == z - radius || blockZ == z + radius)) {
							continue;
						}

						if (blockX == x - (radius - 1) && blockZ == z - radius) {
							rotation = 1;
						}

						if (blockX == x - radius && blockZ == z - (radius - 1)) {
							rotation = 1;
						}

						if (blockX == x + (radius - 1) && blockZ == z - radius) {
							rotation = 3;
						}

						if (blockX == x + radius && blockZ == z - (radius - 1)) {
							rotation = 3;
						}

						if (blockX == x - (radius - 1) && blockZ == z + radius) {
							rotation = 7;
						}

						if (blockX == x - radius && blockZ == z + (radius - 1)) {
							rotation = 7;
						}

						if (blockX == x + (radius - 1) && blockZ == z + radius) {
							rotation = 9;
						}

						if (blockX == x + radius && blockZ == z + (radius - 1)) {
							rotation = 9;
						}
					}

					if (rotation == 5 && blockY < y + height) {
						rotation = 0;
					}

					if ((rotation != 0 || y >= y + height - 1)
							&& !world.getBlock(blockX, blockY, blockZ).func_149730_j()) {
						this.setBlock(world, blockX, blockY, blockZ,
								Block.getBlockById(Block.getIdFromBlock(Blocks.brown_mushroom_block) + genType),
								rotation);
					}
				}
			}
		}

		for (int yRel = 0; yRel < height; ++yRel) {
			Block currentBlock = world.getBlock(x, y + yRel, z);

			if (!currentBlock.func_149730_j()) {
				this.setBlock(world, x, y + yRel, z,
						Block.getBlockById(Block.getIdFromBlock(Blocks.brown_mushroom_block) + genType), 10);
			}
		}

		return true;
	}
}
