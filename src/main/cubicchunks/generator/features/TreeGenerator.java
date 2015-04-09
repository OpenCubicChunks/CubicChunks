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
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public abstract class TreeGenerator extends SurfaceFeatureGenerator {
	
	private final IBlockState woodBlock;
	private final IBlockState leafBlock;

	protected final int attempts;
	protected final double probability;
	
	public TreeGenerator(final World world, final IBlockState woodBlock, final IBlockState leafBlock, int attempts, double probability) {
		super(world);
		this.woodBlock = woodBlock;
		this.leafBlock = leafBlock;
		
		this.attempts = attempts;
		this.probability = probability;
	}
	
	@Override
	public int getAttempts(Random rand) {
		int realAttempts = 0;
		//TODO: Find faster way to calculate it?
		for(int i = 0; i < this.attempts; i++){
			if(rand.nextDouble() <= this.probability){
				realAttempts++;
			}
		}
		return realAttempts;
	}
	
	protected boolean canReplaceBlock(final Block blockToCheck) {
		return testMaterialsForReplacement(blockToCheck) || testBlocksForReplacement(blockToCheck);
	}
	
	protected boolean tryToPlaceDirtUnderTree(final World world, final BlockPos blockPos) {
		if(world.getBlockStateAt(blockPos).getBlock() != Blocks.DIRT) {
			return this.setBlockOnly(blockPos, Blocks.DIRT.getDefaultState());
		} else {
			// it's already dirt, so just say it was placed successfully
			return true;
		}
	}
	
	private boolean testMaterialsForReplacement(final Block blockToCheck) {
		final Material blockMaterial = blockToCheck.getMaterial();
		return blockMaterial == Material.AIR
				|| blockMaterial == Material.LEAVES;
	}
	
	private boolean testBlocksForReplacement(final Block blockToCheck) {
		return blockToCheck == Blocks.GRASS
				|| blockToCheck == Blocks.DIRT
				|| blockToCheck == Blocks.LOG
				|| blockToCheck == Blocks.LOG2
				|| blockToCheck == Blocks.SAPLING
				|| blockToCheck == Blocks.VINE;
	}

	public IBlockState getWoodBlock() {
		return woodBlock;
	}

	public IBlockState getLeafBlock() {
		return leafBlock;
	}

}
