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
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class SimpleTreeGenerator extends SurfaceFeatureGenerator{

	private static final int MIN_TRUNK_HEIGHT = 3;
	private static final int MAX_TRUNK_HEIGHT = 5;//inclusive
	
	private final IBlockState wood;
	private final IBlockState leaves;
	
	public SimpleTreeGenerator(World world, IBlockState wood, IBlockState leaves) {
		super(world);
		this.wood = wood;
		this.leaves = leaves;
	}

	@Override
	public int getAttempts(Random rand, Biome biome) {
		return biome.biomeDecorator.treesPerChunk;
	}

	@Override
	public void generateAt(Random rand, BlockPos pos, Biome biome) {
		final int trunkHeight = rand.nextInt(MAX_TRUNK_HEIGHT + 1 - MIN_TRUNK_HEIGHT) + MIN_TRUNK_HEIGHT;
		
		final int treeHeight = trunkHeight + 1;
		//TODO: tweak these values
		final int treeRadius = 3;
		final int leavesHeight = 4;
		
		if(!isEnoughSpace(pos, treeHeight, leavesHeight, treeRadius)){
			//we don't have enough space
			return;
		}
		generateTree(pos, trunkHeight, treeHeight, leavesHeight, treeRadius);
	}

	private boolean isEnoughSpace(BlockPos pos, int treeHeight, int leavesHeight, int treeRadius) {
		int noLeavesHeight = treeHeight - leavesHeight;
		for(int i = 0; i < noLeavesHeight; i++){
			if(getBlock(pos).getBlock().isSolid()){
				return false;
			}
			pos.add(0, 1, 0);
		}
		
		for(int x = -treeRadius; x <= treeRadius; x++){
			for(int y = 0; y < leavesHeight; y++){
				for(int z = -treeRadius; z <= treeRadius; z++){
					BlockPos currentPos = pos.add(x, y, z);
					if(getBlock(currentPos).getBlock().isSolid()){
						return false;
					}
				}
			}
		}
		return true;
	}

	private void generateTree(BlockPos pos, int trunkHeight, int treeHeight, int leavesHeight, int treeRadius) {
		//generate trunk
		BlockPos currentPos = pos;
		for(int i = 0; i < trunkHeight; i++){
			setBlock(currentPos, wood);
			currentPos = currentPos.add(0, 1, 0);
		}
		
		//generate leaves
		BlockPos startPos = pos.add(0, treeHeight - leavesHeight, 0);
		for(int yRel = 0; yRel < leavesHeight; yRel++){
			int y2 = yRel >> 1 << 1;
			double radiusSubstract = 0.7 * treeRadius * y2/(double)leavesHeight;
			double radius = treeRadius - radiusSubstract;
			this.generateCircleLayerAt(startPos.add(0, yRel, 0), radius);
		}
	}

	private void generateCircleLayerAt(BlockPos pos, double radius) {
		double radiusSquared = radius * radius;
		int r = MathHelper.ceil(radius);
		for(int x = -r; x <= r; x++){
			for(int z = -r; z <= r; z++){
				if(x*x + z*z > radiusSquared){
					continue;
				}
				BlockPos currentPos = pos.add(x, 0, z);
				//don't replace wood
				if(getBlock(currentPos).getBlock().isSolid()){
					continue;
				}
				setBlock(currentPos, leaves);
			}
		}
	}
}
