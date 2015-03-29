/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.generator.biome.biomegen;

import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.world.World;

public class BiomeGenJungle extends CubeBiomeGenBase {
	
	private boolean field_150614_aC;
	
	public BiomeGenJungle(int p_i45379_1_, boolean p_i45379_2_) {
		super(p_i45379_1_);
		this.field_150614_aC = p_i45379_2_;
		
		if (p_i45379_2_) {
			this.theBiomeDecorator.treesPerChunk = 2;
		} else {
			this.theBiomeDecorator.treesPerChunk = 50;
		}
		
		this.theBiomeDecorator.grassPerChunk = 25;
		this.theBiomeDecorator.flowersPerChunk = 4;
		
		if (!p_i45379_2_) {
			this.spawnableMonsterList.add(new CubeBiomeGenBase.SpawnListEntry(EntityOcelot.class, 2, 1, 1));
		}
		
		this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityChicken.class, 10, 4, 4));
	}
	
	public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_) {
		return (WorldGenAbstractTree) (p_150567_1_.nextInt(10) == 0 ? this.worldGeneratorBigTree : (p_150567_1_.nextInt(2) == 0 ? new WorldGenShrub(3, 0) : (!this.field_150614_aC && p_150567_1_.nextInt(3) == 0 ? new WorldGenMegaJungle(false, 10, 20, 3, 3) : new WorldGenTrees(false, 4 + p_150567_1_.nextInt(7), 3, 3, true))));
	}
	
	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	public WorldGenerator getRandomWorldGenForGrass(Random par1Random) {
		return par1Random.nextInt(4) == 0 ? new WorldGenTallGrass(Blocks.tallgrass, 2) : new WorldGenTallGrass(Blocks.tallgrass, 1);
	}
	
	public void decorate(World par1World, Random par2Random, int par3, int par4) {
		super.decorate(par1World, par2Random, par3, par4);
		int var5 = par3 + par2Random.nextInt(16) + 8;
		int var6 = par4 + par2Random.nextInt(16) + 8;
		int var7 = par2Random.nextInt(par1World.getHeightValue(var5, var6) * 2);
		(new WorldGenMelon()).generate(par1World, par2Random, var5, var7, var6);
		WorldGenVines var10 = new WorldGenVines();
		
		for (var6 = 0; var6 < 50; ++var6) {
			var7 = par3 + par2Random.nextInt(16) + 8;
			short var8 = 128;
			int var9 = par4 + par2Random.nextInt(16) + 8;
			var10.generate(par1World, par2Random, var7, var8, var9);
		}
	}
}
