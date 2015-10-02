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

import cubicchunks.generator.features.SurfaceFeatureGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class TreeGenerator extends SurfaceFeatureGenerator {
	
	private static final Block[] REPLACABLE_OPEN_BLOCKS = { Blocks.air, Blocks.sapling, Blocks.flowing_water, Blocks.water, Blocks.flowing_lava,
		Blocks.lava, Blocks.log, Blocks.log2, Blocks.leaves, Blocks.leaves2 };
	
	private static final Block[] REPLACABLE_SOLID_BLOCKS = { Blocks.grass, Blocks.dirt, Blocks.sand, Blocks.gravel };
	
	protected static boolean canReplaceBlockDefault(final Block blockToCheck) {
		return canReplace(blockToCheck, REPLACABLE_OPEN_BLOCKS) || canReplace(blockToCheck, REPLACABLE_SOLID_BLOCKS);
	}
	
	protected static boolean canReplace(final Block blockToCheck, final Block[] blockArray) {
		final Material blockMaterial = blockToCheck.getMaterial();
		
		for(Block block : blockArray){
				if(blockMaterial == block.getMaterial()){
					return true;
				}
		}
		return false;
	}
	
	protected static boolean canReplaceWithLeaves(final Block blockToCheck) {
		final Material blockMaterial = blockToCheck.getMaterial();
		return blockMaterial == Material.air || blockMaterial == Material.leaves;
	}

	protected static boolean canReplaceWithWood(final Block blockToCheck) {
		return blockToCheck == Blocks.grass || blockToCheck == Blocks.dirt || blockToCheck == Blocks.log
				|| blockToCheck == Blocks.log2 || blockToCheck == Blocks.sapling || blockToCheck == Blocks.vine;
	}

	protected final IBlockState woodBlock;
	protected final IBlockState leafBlock;

	public TreeGenerator(final World world, final IBlockState woodBlock, final IBlockState leafBlock) {
		super(world);
		this.woodBlock = woodBlock;
		this.leafBlock = leafBlock;
	}

	protected boolean tryToPlaceDirtUnderTree(final World world, final BlockPos blockPos) {
		if (world.getBlockState(blockPos).getBlock() != Blocks.dirt) {
			return this.setBlockOnly(blockPos, Blocks.dirt.getDefaultState());
		} else {
			// it's already dirt, so just say it was placed successfully
			return true;
		}
	}
}
