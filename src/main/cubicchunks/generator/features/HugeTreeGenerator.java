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
package cubicchunks.generator.features;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class HugeTreeGenerator extends TreeGenerator {

	private final int baseHeight;
	private final int heightRange;

	public HugeTreeGenerator(World world, int baseHeight, int heightRange, IBlockState woodBlock,
			IBlockState leafBlock, int attempts, double prob) {
		super(world, woodBlock, leafBlock, attempts, prob);
		this.baseHeight = baseHeight;
		this.heightRange = heightRange;
	}

	private int getRandomHeight(final Random rand) {
		int result = rand.nextInt(3) + this.baseHeight;

		if (this.heightRange > 1) {
			result += rand.nextInt(this.heightRange);
		}
		return result;
	}

	private boolean canGenerateOnBlocksBelow(final World world, final BlockPos blockPos) {
		BlockPos groundPos = blockPos.below();
		Block groundBlock = world.getBlockStateAt(groundPos).getBlock();

		if (groundBlock == Blocks.GRASS || groundBlock == Blocks.DIRT) {
			return this.tryToPlaceDirtUnderHugeTree(world, groundPos);
		} else {
			return false;
		}
	}

	private boolean tryToPlaceDirtUnderHugeTree(final World world, final BlockPos blockPos) {
		BlockPos groundPos = blockPos.below();

		return this.tryToPlaceDirtUnderTree(world, groundPos) && this.tryToPlaceDirtUnderTree(world, groundPos.east())
				&& this.tryToPlaceDirtUnderTree(world, groundPos.south())
				&& this.tryToPlaceDirtUnderTree(world, groundPos.south().east());
	}

	protected boolean canGenerate(final World world, final Random rand, final BlockPos blockPos, final int height) {
		return this.isEnoughSpaceToGenerate(world, blockPos, height) && this.canGenerateOnBlocksBelow(world, blockPos);
	}

	private boolean isEnoughSpaceToGenerate(World world, BlockPos blockPos, int height) {
		final int baseY = blockPos.getY();

		for (int yAbs = baseY; yAbs <= baseY + 1 + height; ++yAbs) {
			byte radius = 2;

			if (yAbs == baseY) {
				radius = 1;
			} else if (yAbs >= baseY + 1 + height - 2) {
				radius = 2;
			}

			for (int xAbs = blockPos.getX() - radius; xAbs <= blockPos.getX() + radius; ++xAbs) {
				for (int zAbs = blockPos.getZ() - radius; zAbs <= blockPos.getZ() + radius; ++zAbs) {
					Block block = world.getBlockStateAt(new BlockPos(xAbs, yAbs, zAbs)).getBlock();

					if (!this.canReplaceBlock(block)) {
						return false;
					}
				}
			}
		}
		// haven't returned a false, so must be able to generate
		return true;
	}

	protected void generateLargeLeafLayer(final BlockPos blockPos, final int radius) {
		int r2 = radius * radius;

		for (int xAbs = -radius; xAbs <= radius + 1; ++xAbs) {
			for (int zAbs = -radius; zAbs <= radius + 1; ++zAbs) {
				int xDist = xAbs - 1;
				int zDist = zAbs - 1;

				if (xAbs * xAbs + zAbs * zAbs <= r2 || xDist * xDist + zDist * zDist <= r2
						|| xAbs * xAbs + zDist * zDist <= r2 || xDist * xDist + zAbs * zAbs <= r2) {
					BlockPos newPos = blockPos.add(xAbs, 0, zAbs);
					Material material = this.world.getBlockStateAt(newPos).getBlock().getMaterial();

					if (material == Material.AIR || material == Material.LEAVES) {
						this.setBlockOnly(newPos, this.getLeafBlock());
					}
				}
			}
		}
	}

	protected void generateSmallLeafLayer(final BlockPos blockPos, final int radius) {
		int r2 = radius * radius;

		for (int xAbs = -radius; xAbs <= radius; ++xAbs) {
			for (int zAbs = -radius; zAbs <= radius; ++zAbs) {
				if (xAbs * xAbs + zAbs * zAbs <= r2) {
					BlockPos newPos = blockPos.add(xAbs, 0, zAbs);
					Material material = this.world.getBlockStateAt(newPos).getBlock().getMaterial();

					if (material == Material.AIR || material == Material.LEAVES) {
						this.setBlockOnly(newPos, this.getLeafBlock());
					}
				}
			}
		}
	}
}
