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
package cubicchunks.generator.biome;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.Biome;
import cubicchunks.generator.biome.biomegen.CCBiome;

public class CCBiomeManagerFlat extends CCBiomeManager {
	
	/** The biome generator object. */
	private CCBiome biomeGenerator;
	
	/** The rainfall in the world */
	private float rainfall; // this is hell, there IS no rain.
	
	public CCBiomeManagerFlat(CCBiome p_i45374_1_, float p_i45374_2_) {
		this.biomeGenerator = p_i45374_1_;
		this.rainfall = p_i45374_2_;
	}
	
	/**
	 * Returns the BiomeGenBase related to the x, z position on the world.
	 */
	public CCBiome getBiomeGenAt(int par1, int par2) {
		return this.biomeGenerator;
	}
	
	/**
	 * Returns an array of biomes for the location input.
	 * 
	 * It ignores the cubeX, cubeZ input since it doesn't care, it's exactly the same biome everywhere in the nether.
	 */
	public CCBiome[] getBiomesForGeneration(CCBiome[] aBiomeGenBase, int par2, int par3, int width, int length) {
		if (aBiomeGenBase == null || aBiomeGenBase.length < width * length) {
			aBiomeGenBase = new CCBiome[width * length];
		}
		
		Arrays.fill(aBiomeGenBase, 0, width * length, this.biomeGenerator); // fills the array with biome 0. same biome everywhere, then.
		return aBiomeGenBase;
	}
	
	/**
	 * Returns a list of rainfall values for the specified blocks. Args: listToReuse, x, z, width, length.
	 */
	public float[] getRainfall(float[] aFloat, int par2, int par3, int width, int length) {
		if (aFloat == null || aFloat.length < width * length) {
			aFloat = new float[width * length];
		}
		
		Arrays.fill(aFloat, 0, width * length, this.rainfall);
		return aFloat;
	}
	
	/**
	 * Returns biomes to use for the blocks and loads the other data like temperature and humidity onto the WorldChunkManager Args: oldBiomeList, x, z, width, depth
	 */
	public CCBiome[] loadBlockGeneratorData(CCBiome[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4, int par5) {
		if (par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5) {
			par1ArrayOfBiomeGenBase = new CCBiome[par4 * par5];
		}
		
		Arrays.fill(par1ArrayOfBiomeGenBase, 0, par4 * par5, this.biomeGenerator);
		return par1ArrayOfBiomeGenBase;
	}
	
	/**
	 * Return a list of biomes for the specified blocks. Args: listToReuse, x, y, width, length, cacheFlag (if false, don't check biomeCache to avoid infinite loop in BiomeCacheBlock)
	 */
	public CCBiome[] getBiomeGenAt(CCBiome[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4, int par5, boolean par6) {
		return this.loadBlockGeneratorData(par1ArrayOfBiomeGenBase, par2, par3, par4, par5);
	}
	
	@Override
	public BlockPos getRandomPositionInBiome(int p_150795_1_, int p_150795_2_, int p_150795_3_, List<Biome> p_150795_4_, Random p_150795_5_) {
		return p_150795_4_.contains(this.biomeGenerator) ? new BlockPos(p_150795_1_ - p_150795_3_ + p_150795_5_.nextInt(p_150795_3_ * 2 + 1), 0, p_150795_2_ - p_150795_3_ + p_150795_5_.nextInt(p_150795_3_ * 2 + 1)) : null;
	}
	
	/**
	 * checks given Chunk's Biomes against List of allowed ones
	 */
	public boolean areBiomesViable(int par1, int par2, int par3, List<Biome> biomeList) {
		return biomeList.contains(this.biomeGenerator);
	}
}
