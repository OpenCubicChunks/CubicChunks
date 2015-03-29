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

import net.minecraft.block.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.IcePathGenerator;
import net.minecraft.world.gen.feature.IceSpikeGenerator;
import net.minecraft.world.gen.feature.TreeGenerator;
import net.minecraft.world.gen.feature.TreeGeneratorSpruce;

public class BiomeGenSnow extends CCBiome {
	
	private boolean field_150615_aC;
	private IceSpikeGenerator field_150616_aD = new IceSpikeGenerator();
	private IcePathGenerator field_150617_aE = new IcePathGenerator(4);
	private static final String __OBFID = "CL_00000174";
	
	public BiomeGenSnow(int p_i45378_1_, boolean p_i45378_2_) {
		super(p_i45378_1_);
		this.field_150615_aC = p_i45378_2_;
		
		if (p_i45378_2_) {
			this.topBlock = Blocks.SNOW;
		}
		
		this.spawnableCreatureList.clear();
	}
	
	public void decorate(World par1World, Random par2Random, int par3, int par4) {
		if (this.field_150615_aC) {
			int var5;
			int var6;
			int var7;
			
			for (var5 = 0; var5 < 3; ++var5) {
				var6 = par3 + par2Random.nextInt(16) + 8;
				var7 = par4 + par2Random.nextInt(16) + 8;
				this.field_150616_aD.generate(par1World, par2Random, var6, par1World.getSurfacePosition(var6, var7), var7);
			}
			
			for (var5 = 0; var5 < 2; ++var5) {
				var6 = par3 + par2Random.nextInt(16) + 8;
				var7 = par4 + par2Random.nextInt(16) + 8;
				this.field_150617_aE.generate(par1World, par2Random, var6, par1World.getSurfacePosition(var6, var7), var7);
			}
		}
		
		super.decorate(par1World, par2Random, par3, par4);
	}
	
	@Override
	public TreeGenerator checkSpawnTree(Random p_150567_1_) {
		return new TreeGeneratorSpruce(false);
	}
	
	@Override
	protected CCBiome func_150566_k() {
		CCBiome var1 = (new BiomeGenSnow(this.biomeID + 128, true)).func_150557_a(13828095, true).setBiomeName(this.biomeName + " Spikes").setEnableSnow().setTemperatureAndRainfall(0.0F, 0.5F).setHeightRange(new CCBiome.Height(this.biomeHeight + 0.1F, this.biomeVolatility + 0.1F));
		var1.biomeHeight = this.biomeHeight + 0.3F;
		var1.biomeVolatility = this.biomeVolatility + 0.4F;
		return var1;
	}
}
