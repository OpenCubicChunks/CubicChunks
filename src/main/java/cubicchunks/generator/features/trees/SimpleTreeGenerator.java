/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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
package cubicchunks.generator.features.trees;

import cubicchunks.generator.features.trees.TreeGenerator;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class SimpleTreeGenerator extends TreeGenerator {

	private static final int MIN_TRUNK_HEIGHT = 4;
	private static final int MAX_TRUNK_HEIGHT = 6;// inclusive

	public SimpleTreeGenerator(World world, IBlockState woodBlock, IBlockState leafBlock) {
		super(world, woodBlock, leafBlock);
	}

	@Override
	public void generateAt(Random rand, BlockPos pos, BiomeGenBase biome) {
		Block below = getBlockState(pos.down()).getBlock();
		if (below != Blocks.dirt && below != Blocks.grass) {
			return;
		}
		final int trunkHeight = rand.nextInt(MAX_TRUNK_HEIGHT + 1 - MIN_TRUNK_HEIGHT) + MIN_TRUNK_HEIGHT;

		final int treeHeight = trunkHeight + 1;
		// TODO: tweak these values
		final int treeRadius = 2;
		final int leavesHeight = 4;

		if (canGenerateTree(pos, treeHeight, leavesHeight, treeRadius)) {
			generateTree(pos, trunkHeight, treeHeight, leavesHeight, treeRadius);
		}
	}

	private boolean canGenerateTree(BlockPos pos, int treeHeight, int leavesHeight, int treeRadius) {
		// is there enough space for the tree?
		BlockPos originalPos = pos;
		int noLeavesHeight = treeHeight - leavesHeight;
		for (int i = 0; i < noLeavesHeight; i++) {
			if (!canReplaceBlockDefault(getBlockState(pos).getBlock())) {
				return false;
			}
			pos = pos.up();
		}

		for (int x = -treeRadius; x <= treeRadius; x++) {
			for (int y = 0; y < leavesHeight; y++) {
				for (int z = -treeRadius; z <= treeRadius; z++) {
					BlockPos currentPos = pos.add(x, y, z);
					if (!canReplaceBlockDefault(getBlockState(currentPos).getBlock())) {
						return false;
					}
				}
			}
		}
		return this.tryToPlaceDirtUnderTree(world, originalPos);
	}

	private void generateTree(BlockPos pos, int trunkHeight, int treeHeight, int leavesHeight, int treeRadius) {
		// generate trunk
		BlockPos currentPos = pos;
		for (int i = 0; i < trunkHeight; i++) {
			this.setBlockOnly(currentPos, this.woodBlock);
			currentPos = currentPos.up();
		}

		// generate leaves
		BlockPos startPos = pos.add(0, treeHeight - leavesHeight, 0);
		for (int yRel = 0; yRel < leavesHeight; yRel++) {
			int y2 = yRel >> 1 << 1;
			double radiusSubstract = 0.7 * treeRadius * y2 / (double) leavesHeight;
			double radius = treeRadius - radiusSubstract;
			this.generateLeavesCircleLayerAt(this.leafBlock, startPos.up(yRel), radius + 0.5);
		}
	}
	
	private void generateLeavesCircleLayerAt(IBlockState state, BlockPos pos, double radius) {
		double radiusSquared = radius * radius;
		int r = MathHelper.ceiling_double_int(radius);
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				if (x * x + z * z > radiusSquared) {
					continue;
				}
				BlockPos currentPos = pos.add(x, 0, z);
				// don't replace wood
				if (getBlockState(currentPos).getBlock().isSolidFullCube()) {
					continue;
				}
				this.setBlockOnly(currentPos, this.leafBlock);
			}
		}
	}
	
	private void generateLeavesSphereAt(IBlockState state, BlockPos pos, double radius) {
		double radiusSquared = radius * radius;
		int r = MathHelper.ceiling_double_int(radius);
		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				for (int z = -r; z <= r; z++) {
					if (x*x + y*y + z*z > radiusSquared) {
						continue;
					}
					BlockPos currentPos = pos.add(x, y, z);
					// don't replace wood
					if (!canReplaceWithLeaves(getBlockState(currentPos).getBlock())) {
						continue;
					}
					this.setBlockOnly(currentPos, state);
				}
			}
		}
	}
}
