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

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.world.World;
import cubicchunks.world.Cube;

public class BiomeGenMutated extends CubeBiomeGenBase {
	
	protected CubeBiomeGenBase biome;
	
	public BiomeGenMutated(int biomeID, CubeBiomeGenBase biome) {
		super(biomeID);
		this.biome = biome;
		this.func_150557_a(biome.color, true);
		this.biomeName = biome.biomeName + " M";
		this.topBlock = biome.topBlock;
		this.fillerBlock = biome.fillerBlock;
		this.field_76754_C = biome.field_76754_C;
		this.biomeHeight = biome.biomeHeight;
		this.biomeVolatility = biome.biomeVolatility;
		this.temperature = biome.temperature;
		this.rainfall = biome.rainfall;
		this.waterColorMultiplier = biome.waterColorMultiplier;
		this.enableSnow = biome.enableSnow;
		this.enableRain = biome.enableRain;
		this.spawnableCreatureList = new ArrayList(biome.spawnableCreatureList);
		this.spawnableMonsterList = new ArrayList(biome.spawnableMonsterList);
		this.spawnableCaveCreatureList = new ArrayList(biome.spawnableCaveCreatureList);
		this.spawnableWaterCreatureList = new ArrayList(biome.spawnableWaterCreatureList);
		this.temperature = biome.temperature;
		this.rainfall = biome.rainfall;
		this.biomeHeight = biome.biomeHeight + 0.1F;
		this.biomeVolatility = biome.biomeVolatility + 0.2F;
	}
	
	public void decorate(World par1World, Random rand, int par3, int par4) {
		this.biome.theBiomeDecorator.func_150512_a(par1World, rand, this, par3, par4);
	}
	
	public void replaceBlocks_pre(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double var) {
		this.biome.replaceBlocks_pre(world, rand, cube, xAbs, yAbs, zAbs, var);
	}
	
	/**
	 * returns the chance a creature has to spawn.
	 */
	public float getSpawningChance() {
		return this.biome.getSpawningChance();
	}
	
	public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_) {
		return this.biome.checkSpawnTree(p_150567_1_);
	}
	
	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_) {
		return this.biome.getBiomeFoliageColor(p_150571_1_, p_150571_2_, p_150571_2_);
	}
	
	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_) {
		return this.biome.getBiomeGrassColor(p_150558_1_, p_150558_2_, p_150558_2_);
	}
	
	public Class func_150562_l() {
		return this.biome.func_150562_l();
	}
	
	public boolean func_150569_a(CubeBiomeGenBase biome) {
		return this.biome.func_150569_a(biome);
	}
	
	public CubeBiomeGenBase.TempCategory func_150561_m() {
		return this.biome.func_150561_m();
	}
}
