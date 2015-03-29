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
package cubicchunks.generator.biome.biomegen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.world.World;
import cubicchunks.world.Cube;
import net.minecraft.util.BlockPos;
import net.minecraft.world.gen.feature.TreeGenerator;

public class BiomeSwamplands extends CCBiome {
	
	private static final String __OBFID = "CL_00000185";
	
	protected BiomeSwamplands(int par1) {
		super(par1);
		this.theBiomeDecorator.treesPerChunk = 2;
		this.theBiomeDecorator.flowersPerChunk = 1;
		this.theBiomeDecorator.deadBushPerChunk = 1;
		this.theBiomeDecorator.mushroomsPerChunk = 8;
		this.theBiomeDecorator.reedsPerChunk = 10;
		this.theBiomeDecorator.clayPerChunk = 1;
		this.theBiomeDecorator.waterlilyPerChunk = 4;
		this.theBiomeDecorator.sandPerChunk2 = 0;
		this.theBiomeDecorator.sandPerChunk = 0;
		this.theBiomeDecorator.grassPerChunk = 5;
		this.waterColorMultiplier = 14745518;
		this.spawnableMonsterList.add(new SpawnMob(EntitySlime.class, 1, 1, 1));
	}
	
	@Override
	public TreeGenerator checkSpawnTree(Random rand) {
		return this.worldGeneratorSwamp;
	}
	
	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	@Override
	public int getGrassColorAt(final BlockPos a1) {
		double var4 = RAINFALL_NOISE_GENERATOR.getValue(a1.getX() * 0.0225, a1.getZ() * 0.0225);
		return var4 < -0.1D ? 5011004 : 6975545;
	}
	
	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	@Override
	public int getLeavesColorAt(final BlockPos pos) {
		return 6975545;
	}
	
	@Override
	public BlockFlower.FlowerTypes getRandomFlower(Random p_150572_1_, final BlockPos pos) {
		return BlockFlower.FlowerTypes.BLUE_ORCHID;
	}
	
	@Override
	public void replaceBlocks_pre(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double val) {
		double var9 = RAINFALL_NOISE_GENERATOR.getValue((double)xAbs * 0.25D, (double)yAbs * 0.25D);
		
		if (var9 > 0.0D) {
			int xRel = xAbs & 15;
			int yRel = yAbs & 15;
			int zRel = zAbs & 15;
			// int height = blocks.length / 256;
			
			for (int y = 16; y >= 0; --y) {
				int loc = (zRel * 16 + xRel) * 16 + yRel;
				
				Block block = cube.getBlockState(xRel, yRel, zRel).getBlock();
				
				if (block == null || block.getMaterial() != Material.AIR) {
					if (yAbs == 62 && block != Blocks.WATER) {
						cube.setBlockForGeneration(xRel, yRel, zRel, Blocks.WATER);
						
						if (var9 < 0.12D) {
							cube.setBlockForGeneration(xRel, yRel + 1, zRel, Blocks.WATERLILY); // this should always place the lily at a height of 63,
							// and not go into the next cube up which would be bad.
						}
					}
					
					break;
				}
			}
		}
		
		this.replaceBlocks(world, rand, cube, xAbs, yAbs, zAbs, val);
	}
}
