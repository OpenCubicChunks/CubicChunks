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

import cubicchunks.world.Cube;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeGenHills extends CubeBiomeGenBase {
	
	private WorldGenerator theWorldGenerator;
	private WorldGenTaiga2 genTaiga;
	private int value1;
	private int value2;
	private int value3;
	private int value4;
	
	public BiomeGenHills(int biomeID, boolean flag) {
		super(biomeID);
		this.theWorldGenerator = new WorldGenMinable(Blocks.monster_egg, 8);
		this.genTaiga = new WorldGenTaiga2(false);
		this.value1 = 0;
		this.value2 = 1;
		this.value3 = 2;
		this.value4 = this.value1;
		
		if (flag) {
			this.theBiomeDecorator.treesPerChunk = 3;
			this.value4 = this.value2;
		}
	}
	
	public WorldGenAbstractTree checkSpawnTree(Random rand) {
		return (WorldGenAbstractTree) (rand.nextInt(3) > 0 ? this.genTaiga : super.checkSpawnTree(rand));
	}
	
	public void decorate(World world, Random rand, int par3, int par4) {
		super.decorate(world, rand, par3, par4);
		int var5 = 3 + rand.nextInt(6);
		int var6;
		int var7;
		int var8;
		
		for (var6 = 0; var6 < var5; ++var6) {
			var7 = par3 + rand.nextInt(16);
			var8 = rand.nextInt(28) + 4;
			int var9 = par4 + rand.nextInt(16);
			
			if (world.getBlock(var7, var8, var9) == Blocks.stone) {
				world.setBlock(var7, var8, var9, Blocks.emerald_ore, 0, 2);
			}
		}
		
		for (var5 = 0; var5 < 7; ++var5) {
			var6 = par3 + rand.nextInt(16);
			var7 = rand.nextInt(64);
			var8 = par4 + rand.nextInt(16);
			this.theWorldGenerator.generate(world, rand, var6, var7, var8);
		}
	}
	
	public void replaceBlocks_pre(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double var) {
		this.topBlock = Blocks.grass;
		this.field_150604_aj = 0;
		this.fillerBlock = Blocks.dirt;
		
		if ( (var < -1.0D || var > 2.0D) && this.value4 == this.value3) {
			this.topBlock = Blocks.gravel;
			this.fillerBlock = Blocks.gravel;
		} else if (var > 1.0D && this.value4 != this.value2) {
			this.topBlock = Blocks.stone;
			this.fillerBlock = Blocks.stone;
		}
		
		this.replaceBlocks(world, rand, cube, xAbs, yAbs, zAbs, var);
	}
	
	private BiomeGenHills func_150633_b(CubeBiomeGenBase biome) {
		this.value4 = this.value3;
		this.func_150557_a(biome.color, true);
		this.setBiomeName(biome.biomeName + " M");
		this.setHeightRange(new CubeBiomeGenBase.Height(biome.biomeHeight, biome.biomeVolatility));
		this.setTemperatureAndRainfall(biome.temperature, biome.rainfall);
		return this;
	}
	
	protected CubeBiomeGenBase func_150566_k() {
		return (new BiomeGenHills(this.biomeID + 128, false)).func_150633_b(this);
	}
}
