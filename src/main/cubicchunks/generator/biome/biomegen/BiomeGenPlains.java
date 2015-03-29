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

import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;

public class BiomeGenPlains extends CubeBiomeGenBase {
	
	protected boolean field_150628_aC;
	
	protected BiomeGenPlains(int biomeID) {
		super(biomeID);
		this.setTemperatureAndRainfall(0.8F, 0.4F);
		this.setHeightRange(PlainsRange);
		this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityHorse.class, 5, 2, 6));
		this.theBiomeDecorator.treesPerChunk = -999;
		this.theBiomeDecorator.flowersPerChunk = 4;
		this.theBiomeDecorator.grassPerChunk = 10;
	}
	
	public String spawnFlower(Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_) {
		double var5 = field_150606_ad.func_151601_a((double)p_150572_2_ / 200.0D, (double)p_150572_4_ / 200.0D);
		int var7;
		
		if (var5 < -0.8D) {
			var7 = p_150572_1_.nextInt(4);
			return BlockFlower.field_149859_a[4 + var7];
		} else if (p_150572_1_.nextInt(3) > 0) {
			var7 = p_150572_1_.nextInt(3);
			return var7 == 0 ? BlockFlower.field_149859_a[0] : (var7 == 1 ? BlockFlower.field_149859_a[3] : BlockFlower.field_149859_a[8]);
		} else {
			return BlockFlower.field_149858_b[0];
		}
	}
	
	public void decorate(World par1World, Random par2Random, int par3, int par4) {
		double var5 = field_150606_ad.func_151601_a((double) (par3 + 8) / 200.0D, (double) (par4 + 8) / 200.0D);
		int var7;
		int var8;
		int var9;
		int var10;
		
		if (var5 < -0.8D) {
			this.theBiomeDecorator.flowersPerChunk = 15;
			this.theBiomeDecorator.grassPerChunk = 5;
		} else {
			this.theBiomeDecorator.flowersPerChunk = 4;
			this.theBiomeDecorator.grassPerChunk = 10;
			field_150610_ae.func_150548_a(2);
			
			for (var7 = 0; var7 < 7; ++var7) {
				var8 = par3 + par2Random.nextInt(16) + 8;
				var9 = par4 + par2Random.nextInt(16) + 8;
				var10 = par2Random.nextInt(par1World.getHeightValue(var8, var9) + 32);
				field_150610_ae.generate(par1World, par2Random, var8, var10, var9);
			}
		}
		
		if (this.field_150628_aC) {
			field_150610_ae.func_150548_a(0);
			
			for (var7 = 0; var7 < 10; ++var7) {
				var8 = par3 + par2Random.nextInt(16) + 8;
				var9 = par4 + par2Random.nextInt(16) + 8;
				var10 = par2Random.nextInt(par1World.getHeightValue(var8, var9) + 32);
				field_150610_ae.generate(par1World, par2Random, var8, var10, var9);
			}
		}
		
		super.decorate(par1World, par2Random, par3, par4);
	}
	
	protected CubeBiomeGenBase func_150566_k() {
		BiomeGenPlains var1 = new BiomeGenPlains(this.biomeID + 128);
		var1.setBiomeName("Sunflower Plains");
		var1.field_150628_aC = true;
		var1.setColor(9286496);
		var1.field_150609_ah = 14273354;
		return var1;
	}
}
