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

import cubicchunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class WorldGenTreesCube extends WorldGenAbstractTreeCube {
	/** The minimum height of a generated tree. */
	private final int minTreeHeight;

	/** True if this tree should grow Vines. */
	private final boolean vinesGrow;

	/** The metadata value of the wood to use in tree generation. */
	private final int metaWood;

	/** The metadata value of the leaves to use in tree generation. */
	private final int metaLeaves;

	public WorldGenTreesCube(boolean doBlockNotify) {
		this(doBlockNotify, 4, 0, 0, false);
	}

	public WorldGenTreesCube(boolean doBlockNotify, int minTreeHeight, int metaWood, int metaLeaves, boolean vinesGrow) {
		super(doBlockNotify);
		this.minTreeHeight = minTreeHeight;
		this.metaWood = metaWood;
		this.metaLeaves = metaLeaves;
		this.vinesGrow = vinesGrow;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int treeHeight = rand.nextInt(3) + this.minTreeHeight;

		boolean canGenerate = true;

		for (int yAbs = y; yAbs <= y + 1 + treeHeight; ++yAbs) {
			int radius = yAbs >= y + 1 + treeHeight - 2 ? 2 : yAbs == y ? 0 : 1;

			for (int xAbs = x - radius; xAbs <= x + radius && canGenerate; ++xAbs) {
				for (int zAbs = z - radius; zAbs <= z + radius && canGenerate; ++zAbs) {
					Block block = world.getBlock(xAbs, yAbs, zAbs);

					if (!this.isReplacableFromTreeGen(block)) {
						canGenerate = false;
					}
				}
			}
		}

		if (!canGenerate) {
			return false;
		}
		Block blockBelow = world.getBlock(x, y - 1, z);

		if (blockBelow != Blocks.grass && blockBelow != Blocks.dirt && blockBelow != Blocks.farmland) {
			return false;
		}

		this.setBlock(world, x, y - 1, z, Blocks.dirt);
		int noLeavesHeight = 3;
		byte minRadius = 0;

		for (int yAbs = y - noLeavesHeight + treeHeight; yAbs <= y + treeHeight; ++yAbs) {
			int yRel = yAbs - (y + treeHeight);
			// yRel <= 0
			int radius = minRadius + 1 - yRel / 2;

			for (int xAbs = x - radius; xAbs <= x + radius; ++xAbs) {
				int xRel = xAbs - x;

				for (int zAbs = z - radius; zAbs <= z + radius; ++zAbs) {
					int zRel = zAbs - z;

					if (Math.abs(xRel) != radius || Math.abs(zRel) != radius || rand.nextInt(2) != 0 && yRel != 0) {
						Block block = world.getBlock(xAbs, yAbs, zAbs);

						if (block.getMaterial() == Material.air || block.getMaterial() == Material.leaves) {
							this.setBlock(world, xAbs, yAbs, zAbs, Blocks.leaves, this.metaLeaves);
						}
					}
				}
			}
		}

		for (int yRel = 0; yRel < treeHeight; ++yRel) {
			Block block = world.getBlock(x, y + yRel, z);

			if (block.getMaterial() == Material.air || block.getMaterial() == Material.leaves) {
				this.setBlock(world, x, y + yRel, z, Blocks.log, this.metaWood);

				if (this.vinesGrow && yRel > 0) {
					if (rand.nextInt(3) > 0 && world.isAirBlock(x - 1, y + yRel, z)) {
						this.setBlock(world, x - 1, y + yRel, z, Blocks.vine, 8);
					}

					if (rand.nextInt(3) > 0 && world.isAirBlock(x + 1, y + yRel, z)) {
						this.setBlock(world, x + 1, y + yRel, z, Blocks.vine, 2);
					}

					if (rand.nextInt(3) > 0 && world.isAirBlock(x, y + yRel, z - 1)) {
						this.setBlock(world, x, y + yRel, z - 1, Blocks.vine, 1);
					}

					if (rand.nextInt(3) > 0 && world.isAirBlock(x, y + yRel, z + 1)) {
						this.setBlock(world, x, y + yRel, z + 1, Blocks.vine, 4);
					}
				}
			}
		}

		if (!this.vinesGrow) {
			return true;
		}

		for (int yAbs = y - 3 + treeHeight; yAbs <= y + treeHeight; ++yAbs) {
			int yRel = yAbs - (y + treeHeight);
			int radius = 2 - yRel / 2;

			for (int xAbs = x - radius; xAbs <= x + radius; ++xAbs) {
				for (int zAbs = z - radius; zAbs <= z + radius; ++zAbs) {
					if (world.getBlock(xAbs, yAbs, zAbs).getMaterial() == Material.leaves) {
						if (rand.nextInt(4) == 0 && world.getBlock(xAbs - 1, yAbs, zAbs).getMaterial() == Material.air) {
							this.growVines(world, xAbs - 1, yAbs, zAbs, 8);
						}

						if (rand.nextInt(4) == 0 && world.getBlock(xAbs + 1, yAbs, zAbs).getMaterial() == Material.air) {
							this.growVines(world, xAbs + 1, yAbs, zAbs, 2);
						}

						if (rand.nextInt(4) == 0 && world.getBlock(xAbs, yAbs, zAbs - 1).getMaterial() == Material.air) {
							this.growVines(world, xAbs, yAbs, zAbs - 1, 1);
						}

						if (rand.nextInt(4) == 0 && world.getBlock(xAbs, yAbs, zAbs + 1).getMaterial() == Material.air) {
							this.growVines(world, xAbs, yAbs, zAbs + 1, 4);
						}
					}
				}
			}
		}

		if (rand.nextInt(5) == 0 && treeHeight > 5) {
			for (int yRel = 0; yRel < 2; ++yRel) {
				for (int direction = 0; direction < 4; ++direction) {
					if (rand.nextInt(4 - yRel) == 0) {
						int growthStage = rand.nextInt(3);
						this.setBlock(world, x + Direction.offsetX[Direction.rotateOpposite[direction]], y + treeHeight
								- 5 + yRel, z + Direction.offsetZ[Direction.rotateOpposite[direction]], Blocks.cocoa,
								growthStage << 2 | direction);// metadata
					}
				}
			}
		}

		return true;
	}

	/**
	 * Grows vines downward from the given block for a given length. Args:
	 * World, x, starty, z, vine-length
	 */
	private void growVines(World world, int x, int y, int z, int rotation) {
		this.func_150516_a(world, x, y, z, Blocks.vine, rotation);
		int toGenerate = 4;

		while (toGenerate > 0) {
			--toGenerate;
			--y;

			if (world.getBlock(x, y, z).getMaterial() != Material.air) {
				return;
			}

			this.setBlock(world, x, y, z, Blocks.vine, rotation);

		}
	}
}
