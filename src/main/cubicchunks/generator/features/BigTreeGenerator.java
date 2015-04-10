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
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class BigTreeGenerator extends TreeGenerator {

	//TODO: Make BigTreeGenerator constants configurable
	private static final int MIN_TRUNK_HEIGHT = 4;
	private static final int MAX_TRUNK_HEIGHT = 6;//inclusive
	
	private static final int BRANCH_LENGTH = 4;
	private static final int LEAVES_RADIUS = 3;
					
	private static final int MIN_BRANCH_NUMBER = 6;
	private static final int MAX_BRANCH_NUMBER = 8;
	
	
	public BigTreeGenerator(World world, IBlockState woodBlock, IBlockState leafBlock, int attempts, double probability) {
		super(world, woodBlock, leafBlock, attempts, probability);
	}

	@Override
	public void generateAt(Random rand, BlockPos pos, Biome biome) {
		int trunkHeight = rand.nextInt(MAX_TRUNK_HEIGHT - MIN_TRUNK_HEIGHT + 1) + MIN_TRUNK_HEIGHT;
		int treeHeight = trunkHeight + BRANCH_LENGTH + 1;//add 1 for leaves block
		
		//don't scan the entire tree area. This class may be possibly used for bonemeal tree generation
		//and it would be confusing if we sometimes could generate the tree at some location, and sometimes fail
		//try to generate (possibly incomplete) tree if there is some space
		if(!canGenerate(pos, MAX_TRUNK_HEIGHT + BRANCH_LENGTH, 2)){
			return;
		}
		this.generateTrunk(pos, trunkHeight);
		this.generateBranchesAndLeaves(rand, pos.above(trunkHeight));
	}

	private boolean canGenerate(BlockPos pos, int treeHeight, int scanRadius) {
		if(!canReplaceBlock(getBlock(pos).getBlock())){
			return false;
		}
		pos = pos.above();
		
		int scanHeight = treeHeight - 1;
		
		for(int y = 0; y < scanHeight; y++){
			for(int x = 0; x < scanRadius; x++){
				for(int z = 0; z < scanRadius; z++){
					BlockPos currentPos = pos.add(x, y, z);
					if(!this.canReplaceBlock(getBlock(currentPos).getBlock())){
						return false;
					}
				}
			}
		}
		return true;
	}

	private void generateTrunk(BlockPos pos, int trunkHeight) {
		for(int i = 0; i < trunkHeight; i++){
			setBlockOnly(pos, this.getWoodBlock());
			pos = pos.above();
		}
	}

	private void generateBranchesAndLeaves(Random rand, BlockPos startPos) {
		int numBranches = rand.nextInt(MAX_BRANCH_NUMBER - MIN_BRANCH_NUMBER + 1) + MIN_BRANCH_NUMBER;
		for(int i = 0; i < numBranches; i++){
			this.generateBranch(rand, startPos);
		}
	}

	private void generateBranch(Random rand, BlockPos startPos) {
		Vec3 direction = getRandomNormalizedDirection(rand);
		
		Vec3 addPos = new Vec3(0, 0, 0);
		for(int i = 0; i < BRANCH_LENGTH; i++){
			//wtf? no getters?
			BlockPos currentPos = add(startPos, addPos);
			this.setBlockOnly(currentPos, this.getWoodBlock());
			this.generateLeavesSphereAt(currentPos, LEAVES_RADIUS);
			addPos = addPos.addVector(direction);
		}
	}

	private BlockPos add(BlockPos pos, Vec3 vec){
		return pos.add(vec.x + 0.5, vec.y, vec.z + 0.5);
	}
	private Vec3 getRandomNormalizedDirection(Random rand) {
		double yDir = rand.nextDouble();
		//at least one of the values has to be non-zero
		if(yDir == 0){
			//set it to something close enough to zero
			yDir = 0.001;
		}
		double xDir = rand.nextDouble() * 2 - 1;
		double zDir = rand.nextDouble() * 2 - 1;
		Vec3 vec = new Vec3(xDir, yDir, zDir);
		return vec.getUnitVector();
	}
}
