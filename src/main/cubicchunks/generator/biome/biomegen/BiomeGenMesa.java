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

import java.util.Arrays;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import cubicchunks.generator.noise.NoiseGeneratorPerlin;

public class BiomeGenMesa extends CubeBiomeGenBase {
	
	private byte[] field_150621_aC;
	private long field_150622_aD;
	private NoiseGeneratorPerlin noise1;
	private NoiseGeneratorPerlin noise2;
	private NoiseGeneratorPerlin noise3;
	private boolean field_150626_aH;
	private boolean field_150620_aI;
	
	public BiomeGenMesa(int p_i45380_1_, boolean p_i45380_2_, boolean p_i45380_3_) {
		super(p_i45380_1_);
		this.field_150626_aH = p_i45380_2_;
		this.field_150620_aI = p_i45380_3_;
		this.setDisableRain();
		this.setTemperatureAndRainfall(2.0F, 0.0F);
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.field_150604_aj = 1;
		this.fillerBlock = Blocks.stained_hardened_clay;
		this.theBiomeDecorator.treesPerChunk = -999;
		this.theBiomeDecorator.deadBushPerChunk = 20;
		this.theBiomeDecorator.reedsPerChunk = 3;
		this.theBiomeDecorator.cactiPerChunk = 5;
		this.theBiomeDecorator.flowersPerChunk = 0;
		this.spawnableCreatureList.clear();
		
		if (p_i45380_3_) {
			this.theBiomeDecorator.treesPerChunk = 5;
		}
	}
	
	public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_) {
		return this.worldGeneratorTrees;
	}
	
	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_) {
		return 10387789;
	}
	
	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_) {
		return 9470285;
	}
	
	public void decorate(World par1World, Random par2Random, int par3, int par4) {
		super.decorate(par1World, par2Random, par3, par4);
	}
	
	public void modifyBlocks_pre(World world, Random rand, Block[] blocks, byte[] meta, int xAbs, int yAbs, int zAbs, double val) {
		if (this.field_150621_aC == null || this.field_150622_aD != world.getSeed()) {
			this.func_150619_a(world.getSeed());
		}
		
		if (this.noise1 == null || this.noise2 == null || this.field_150622_aD != world.getSeed()) {
			Random var9 = new Random(this.field_150622_aD);
			this.noise1 = new NoiseGeneratorPerlin(var9, 4);
			this.noise2 = new NoiseGeneratorPerlin(var9, 1);
		}
		
		this.field_150622_aD = world.getSeed();
		double var25 = 0.0D;
		int xRel;
		int yRel;
		int zRel;
		
		if (this.field_150626_aH) {
			xRel = (xAbs & -16) + (zAbs & 15);
			zRel = (zAbs & -16) + (xAbs & 15);
			double var13 = Math.min(Math.abs(val), this.noise1.func_151601_a((double)xRel * 0.25D, (double)zRel * 0.25D));
			
			if (var13 > 0.0D) {
				double var15 = 0.001953125D;
				double var17 = Math.abs(this.noise2.func_151601_a((double)xRel * var15, (double)zRel * var15));
				var25 = var13 * var13 * 2.5D;
				double var19 = Math.ceil(var17 * 50.0D) + 14.0D;
				
				if (var25 > var19) {
					var25 = var19;
				}
				
				var25 += 64.0D;
			}
		}
		
		xRel = xAbs & 15;
		yRel = yAbs & 15;
		zRel = zAbs & 15;
		boolean var26 = true;
		Block var14 = Blocks.stained_hardened_clay;
		Block var27 = this.fillerBlock;
		int var16 = (int) (val / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
		boolean var28 = Math.cos(val / 3.0D * Math.PI) > 0.0D;
		int var18 = -1;
		boolean var29 = false;
		int var20 = blocks.length / 256;
		
		for (int y = 16; y >= 0; --y) {
			int loc = (zRel * 16 + xRel) * 16 + y;
			
			if ( (blocks[loc] == null || blocks[loc].getMaterial() == Material.air) && yAbs < (int)var25) {
				blocks[loc] = Blocks.stone;
			}
			
			if (yAbs <= 0 + rand.nextInt(5)) {
				blocks[loc] = Blocks.bedrock;
			} else {
				Block block = blocks[loc];
				
				if (block != null && block.getMaterial() != Material.air) {
					if (block == Blocks.stone) {
						byte var24;
						
						if (var18 == -1) {
							var29 = false;
							
							if (var16 <= 0) {
								var14 = null;
								var27 = Blocks.stone;
							} else if (yAbs >= 59 && y <= 64) {
								var14 = Blocks.stained_hardened_clay;
								var27 = this.fillerBlock;
							}
							
							if (yAbs < 63 && (var14 == null || var14.getMaterial() == Material.air)) {
								var14 = Blocks.water;
							}
							
							var18 = var16 + Math.max(0, y - 63);
							
							if (yAbs >= 62) {
								if (this.field_150620_aI && y > 86 + var16 * 2) {
									if (var28) {
										blocks[loc] = Blocks.dirt;
										meta[loc] = 1;
									} else {
										blocks[loc] = Blocks.grass;
									}
								} else if (yAbs > 66 + var16) {
									var24 = 16;
									
									if (yAbs >= 64 && yAbs <= 127) {
										if (!var28) {
											var24 = this.func_150618_d(xAbs, y, zAbs);
										}
									} else {
										var24 = 1;
									}
									
									if (var24 < 16) {
										blocks[loc] = Blocks.stained_hardened_clay;
										meta[loc] = (byte)var24;
									} else {
										blocks[loc] = Blocks.hardened_clay;
									}
								} else {
									blocks[loc] = this.topBlock;
									meta[loc] = (byte)this.field_150604_aj;
									var29 = true;
								}
							} else {
								blocks[loc] = var27;
								
								if (var27 == Blocks.stained_hardened_clay) {
									meta[loc] = 1;
								}
							}
						} else if (var18 > 0) {
							--var18;
							
							if (var29) {
								blocks[loc] = Blocks.stained_hardened_clay;
								meta[loc] = 1;
							} else {
								var24 = this.func_150618_d(xAbs, y, zAbs);
								
								if (var24 < 16) {
									blocks[loc] = Blocks.stained_hardened_clay;
									meta[loc] = var24;
								} else {
									blocks[loc] = Blocks.hardened_clay;
								}
							}
						}
					}
				} else {
					var18 = -1;
				}
			}
		}
	}
	
	private void func_150619_a(long p_150619_1_) {
		this.field_150621_aC = new byte[64];
		Arrays.fill(this.field_150621_aC, (byte)16);
		Random var3 = new Random(p_150619_1_);
		this.noise3 = new NoiseGeneratorPerlin(var3, 1);
		int var4;
		
		for (var4 = 0; var4 < 64; ++var4) {
			var4 += var3.nextInt(5) + 1;
			
			if (var4 < 64) {
				this.field_150621_aC[var4] = 1;
			}
		}
		
		var4 = var3.nextInt(4) + 2;
		int var5;
		int var6;
		int var7;
		int var8;
		
		for (var5 = 0; var5 < var4; ++var5) {
			var6 = var3.nextInt(3) + 1;
			var7 = var3.nextInt(64);
			
			for (var8 = 0; var7 + var8 < 64 && var8 < var6; ++var8) {
				this.field_150621_aC[var7 + var8] = 4;
			}
		}
		
		var5 = var3.nextInt(4) + 2;
		int var9;
		
		for (var6 = 0; var6 < var5; ++var6) {
			var7 = var3.nextInt(3) + 2;
			var8 = var3.nextInt(64);
			
			for (var9 = 0; var8 + var9 < 64 && var9 < var7; ++var9) {
				this.field_150621_aC[var8 + var9] = 12;
			}
		}
		
		var6 = var3.nextInt(4) + 2;
		
		for (var7 = 0; var7 < var6; ++var7) {
			var8 = var3.nextInt(3) + 1;
			var9 = var3.nextInt(64);
			
			for (int var10 = 0; var9 + var10 < 64 && var10 < var8; ++var10) {
				this.field_150621_aC[var9 + var10] = 14;
			}
		}
		
		var7 = var3.nextInt(3) + 3;
		var8 = 0;
		
		for (var9 = 0; var9 < var7; ++var9) {
			byte var12 = 1;
			var8 += var3.nextInt(16) + 4;
			
			for (int var11 = 0; var8 + var11 < 64 && var11 < var12; ++var11) {
				this.field_150621_aC[var8 + var11] = 0;
				
				if (var8 + var11 > 1 && var3.nextBoolean()) {
					this.field_150621_aC[var8 + var11 - 1] = 8;
				}
				
				if (var8 + var11 < 63 && var3.nextBoolean()) {
					this.field_150621_aC[var8 + var11 + 1] = 8;
				}
			}
		}
	}
	
	private byte func_150618_d(int p_150618_1_, int p_150618_2_, int p_150618_3_) {
		int var4 = (int)Math.round(this.noise3.func_151601_a((double)p_150618_1_ * 1.0D / 512.0D, (double)p_150618_1_ * 1.0D / 512.0D) * 2.0D);
		return this.field_150621_aC[ (p_150618_2_ + var4 + 64) % 64];
	}
	
	protected CubeBiomeGenBase func_150566_k() {
		boolean var1 = this.biomeID == CubeBiomeGenBase.mesa.biomeID;
		BiomeGenMesa var2 = new BiomeGenMesa(this.biomeID + 128, var1, this.field_150620_aI);
		
		if (!var1) {
			var2.setHeightRange(hillsRange);
			var2.setBiomeName(this.biomeName + " M");
		} else {
			var2.setBiomeName(this.biomeName + " (Bryce)");
		}
		
		var2.func_150557_a(this.color, true);
		return var2;
	}
}
