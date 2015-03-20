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
package cubicchunks.generator.biome;

import java.util.ArrayList;
import java.util.List;

import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.LongHashMap;

public class BiomeCache {
	
	/** Reference to the WorldChunkManager */
	private final WorldColumnManager columnManager;
	
	/** The last time this BiomeCache was cleaned, in milliseconds. */
	private long lastCleanupTime;
	
	/**
	 * The map of keys to BiomeCacheBlocks. Keys are based on the column x, z coordinates as (x | z << 32). Well, shit. This basically caches the biomes for the entire world, on a column-by-column basis.
	 */
	private LongHashMap cacheMap = new LongHashMap();
	
	/** The list of cached BiomeCacheBlocks */
	private List<Block> cache = new ArrayList<Block>();
	
	public BiomeCache(WorldColumnManager par1WorldChunkManager) {
		this.columnManager = par1WorldChunkManager;
	}
	
	/**
	 * Returns a biome cache block at location specified.
	 */
	public BiomeCache.Block getBiomeCacheBlock(int xAbs, int zAbs) {
		xAbs >>= 4;
		zAbs >>= 4;
		long var3 = (long)xAbs & 4294967295L | ((long)zAbs & 4294967295L) << 32;
		BiomeCache.Block block = (BiomeCache.Block)this.cacheMap.getValueByKey(var3);
		
		if (block == null) {
			block = new BiomeCache.Block(xAbs, zAbs);
			this.cacheMap.add(var3, block);
			this.cache.add(block);
		}
		
		block.lastAccessTime = MinecraftServer.getSystemTimeMillis();
		return block;
	}
	
	/**
	 * Returns the BiomeGenBase related to the x, z position from the cache.
	 */
	public CubeBiomeGenBase getBiomeGenAt(int xAbs, int zAbs) {
		return this.getBiomeCacheBlock(xAbs, zAbs).getBiomeGenAt(xAbs, zAbs);
	}
	
	/**
	 * Removes BiomeCacheBlocks from this cache that haven't been accessed in at least 30 seconds.
	 */
	public void cleanupCache() {
		long curTime = MinecraftServer.getSystemTimeMillis();
		long elapsed = curTime - this.lastCleanupTime;
		
		if (elapsed > 7500L || elapsed < 0L) {
			this.lastCleanupTime = curTime;
			
			for (int i = 0; i < this.cache.size(); ++i) {
				BiomeCache.Block block = this.cache.get(i);
				long elapsed2 = curTime - block.lastAccessTime;
				
				if (elapsed2 > 30000L || elapsed2 < 0L) {
					this.cache.remove(i--);
					long coord = (long)block.xPosition & 4294967295L | ((long)block.zPosition & 4294967295L) << 32;
					this.cacheMap.remove(coord);
				}
			}
		}
	}
	
	/**
	 * Returns the array of cached biome types in the BiomeCacheBlock at the given location.
	 */
	public CubeBiomeGenBase[] getCachedBiomes(int xAbs, int zAbs) {
		return this.getBiomeCacheBlock(xAbs, zAbs).biomes;
	}
	
	public class Block {
		
		public float[] rainfallValues = new float[256];
		public CubeBiomeGenBase[] biomes = new CubeBiomeGenBase[256];
		public int xPosition;
		public int zPosition;
		public long lastAccessTime;
		
		public Block(int cubeX, int cubeZ) {
			this.xPosition = cubeX;
			this.zPosition = cubeZ;
			BiomeCache.this.columnManager.getRainfall(this.rainfallValues, cubeX << 4, cubeZ << 4, 16, 16);
			BiomeCache.this.columnManager.getBiomeGenAt(this.biomes, cubeX << 4, cubeZ << 4, 16, 16, false);
		}
		
		public CubeBiomeGenBase getBiomeGenAt(int xAbs, int zAbs) {
			return this.biomes[xAbs & 15 | (zAbs & 15) << 4];
		}
	}
}
