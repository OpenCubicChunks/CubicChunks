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
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BiomeGenForest extends CubeBiomeGenBase {
	
	private int field_150632_aF;
	protected static final WorldGenForest field_150629_aC = new WorldGenForest(false, true);
	protected static final WorldGenForest field_150630_aD = new WorldGenForest(false, false);
	protected static final WorldGenCanopyTree field_150631_aE = new WorldGenCanopyTree(false);
	
	public BiomeGenForest(int biomeID, int variant) {
		super(biomeID);
		this.field_150632_aF = variant;
		this.theBiomeDecorator.treesPerChunk = 10;
		this.theBiomeDecorator.grassPerChunk = 2;
		
		if (this.field_150632_aF == 1) {
			this.theBiomeDecorator.treesPerChunk = 6;
			this.theBiomeDecorator.flowersPerChunk = 100;
			this.theBiomeDecorator.grassPerChunk = 1;
		}
		
		this.func_76733_a(5159473);
		this.setTemperatureAndRainfall(0.7F, 0.8F);
		
		if (this.field_150632_aF == 2) {
			this.field_150609_ah = 353825;
			this.color = 3175492;
			this.setTemperatureAndRainfall(0.6F, 0.6F);
		}
		
		if (this.field_150632_aF == 0) {
			this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityWolf.class, 5, 4, 4));
		}
		
		if (this.field_150632_aF == 3) {
			this.theBiomeDecorator.treesPerChunk = -999;
		}
	}
	
	protected CubeBiomeGenBase func_150557_a(int p_150557_1_, boolean p_150557_2_) {
		if (this.field_150632_aF == 2) {
			this.field_150609_ah = 353825;
			this.color = p_150557_1_;
			
			if (p_150557_2_) {
				this.field_150609_ah = (this.field_150609_ah & 16711422) >> 1;
			}
			
			return this;
		} else {
			return super.func_150557_a(p_150557_1_, p_150557_2_);
		}
	}
	
	public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_) {
		return (WorldGenAbstractTree) (this.field_150632_aF == 3 && p_150567_1_.nextInt(3) > 0 ? field_150631_aE : (this.field_150632_aF != 2 && p_150567_1_.nextInt(5) != 0 ? this.worldGeneratorTrees : field_150630_aD));
	}
	
	public String spawnFlower(Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_) {
		if (this.field_150632_aF == 1) {
			double var5 = MathHelper.clamp_double( (1.0D + field_150606_ad.func_151601_a((double)p_150572_2_ / 48.0D, (double)p_150572_4_ / 48.0D)) / 2.0D, 0.0D, 0.9999D);
			int var7 = (int) (var5 * (double)BlockFlower.field_149859_a.length);
			
			if (var7 == 1) {
				var7 = 0;
			}
			
			return BlockFlower.field_149859_a[var7];
		} else {
			return super.spawnFlower(p_150572_1_, p_150572_2_, p_150572_3_, p_150572_4_);
		}
	}
	
	public void decorate(World par1World, Random par2Random, int par3, int par4) {
		int var5;
		int var6;
		int var7;
		int var8;
		int var9;
		
		if (this.field_150632_aF == 3) {
			for (var5 = 0; var5 < 4; ++var5) {
				for (var6 = 0; var6 < 4; ++var6) {
					var7 = par3 + var5 * 4 + 1 + 8 + par2Random.nextInt(3);
					var8 = par4 + var6 * 4 + 1 + 8 + par2Random.nextInt(3);
					var9 = par1World.getHeightValue(var7, var8);
					
					if (par2Random.nextInt(20) == 0) {
						WorldGenBigMushroom var10 = new WorldGenBigMushroom();
						var10.generate(par1World, par2Random, var7, var9, var8);
					} else {
						WorldGenAbstractTree var12 = this.checkSpawnTree(par2Random);
						var12.setScale(1.0D, 1.0D, 1.0D);
						
						if (var12.generate(par1World, par2Random, var7, var9, var8)) {
							var12.func_150524_b(par1World, par2Random, var7, var9, var8);
						}
					}
				}
			}
		}
		
		var5 = par2Random.nextInt(5) - 3;
		
		if (this.field_150632_aF == 1) {
			var5 += 2;
		}
		
		var6 = 0;
		
		while (var6 < var5) {
			var7 = par2Random.nextInt(3);
			
			if (var7 == 0) {
				field_150610_ae.func_150548_a(1);
			} else if (var7 == 1) {
				field_150610_ae.func_150548_a(4);
			} else if (var7 == 2) {
				field_150610_ae.func_150548_a(5);
			}
			
			var8 = 0;
			
			while (true) {
				if (var8 < 5) {
					var9 = par3 + par2Random.nextInt(16) + 8;
					int var13 = par4 + par2Random.nextInt(16) + 8;
					int var11 = par2Random.nextInt(par1World.getHeightValue(var9, var13) + 32);
					
					if (!field_150610_ae.generate(par1World, par2Random, var9, var11, var13)) {
						++var8;
						continue;
					}
				}
				
				++var6;
				break;
			}
		}
		
		super.decorate(par1World, par2Random, par3, par4);
	}
	
	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_) {
		int var4 = super.getBiomeGrassColor(p_150558_1_, p_150558_2_, p_150558_3_);
		return this.field_150632_aF == 3 ? (var4 & 16711422) + 2634762 >> 1 : var4;
	}
	
	protected CubeBiomeGenBase func_150566_k() {
		if (this.biomeID == CubeBiomeGenBase.forest.biomeID) {
			BiomeGenForest var1 = new BiomeGenForest(this.biomeID + 128, 1);
			var1.setHeightRange(new CubeBiomeGenBase.Height(this.biomeHeight, this.biomeVolatility + 0.2F));
			var1.setBiomeName("Flower Forest");
			var1.func_150557_a(6976549, true);
			var1.func_76733_a(8233509);
			return var1;
		} else {
			return this.biomeID != CubeBiomeGenBase.birchForest.biomeID && this.biomeID != CubeBiomeGenBase.birchForestHills.biomeID ? new BiomeGenMutated(this.biomeID + 128, this) {
				
				public void decorate(World var1, Random var2, int var3, int var4) {
					this.biome.decorate(var1, var2, var3, var4);
				}
			} : new BiomeGenMutated(this.biomeID + 128, this) {
				
				public WorldGenAbstractTree checkSpawnTree(Random var1) {
					return var1.nextBoolean() ? BiomeGenForest.field_150629_aC : BiomeGenForest.field_150630_aD;
				}
			};
		}
	}
}
