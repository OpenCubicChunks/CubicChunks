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

import cubicchunks.generator.biome.biomegen.CCBiome;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;

public class CCBiomeManager extends BiomeManager {

	private final List<Biome> spawnableBiomes = new ArrayList<Biome>();;
	
	protected CCBiomeManager() {
		super();
		this.init();
	}
	
	public CCBiomeManager(long par1, DimensionType dimensionType, String customSettings) {
		super(par1, dimensionType, customSettings);
		this.init();
	}
	
	public CCBiomeManager(World world) {
		this(world.getSeed(), world.getWorldInfo().getDimensionType(), null);
	}
	
	private void init() {
		this.spawnableBiomes.add( CCBiome.forest );
		this.spawnableBiomes.add( CCBiome.plains );
		this.spawnableBiomes.add( CCBiome.taiga );
		this.spawnableBiomes.add( CCBiome.taigaHills );
		this.spawnableBiomes.add( CCBiome.forestHills );
		this.spawnableBiomes.add( CCBiome.jungle );
		this.spawnableBiomes.add( CCBiome.jungleHills );
	}
	
	/**
	 * Gets the list of valid biomes for the player to spawn in.
	 */
	@Override
	public List<Biome> getSpawnableBiomes() {
		return this.spawnableBiomes;
	}
	
	/**
	 * Returns the BiomeGenBase related to the x, z position on the world.
	 */
	@Override
	public Biome getBiome(BlockPos pos) {
		return CCBiome.getBiome(super.getBiome(pos).biomeID);
	}

	/**
	 * Returns an array of biomes for the location input.
	 */
	@Override
	public Biome[] getBiomeMap2(Biome[] biomes, int cubeX, int cubeZ, int width, int length) {
		Biome vanillaBiomes[] = super.getBiomeMap2(biomes, cubeX, cubeZ, width, length);
		return this.convertToCubeBiomes(vanillaBiomes);
	}
	
	/**
	 * Returns biomes to use for the blocks and loads the other data like temperature and humidity onto the WorldChunkManager Args: oldBiomeList, x, z, width, depth
	 */
	@Override
	public Biome[] getBiomeMap(Biome[] biomes, int cubeX, int cubeZ, int width, int length) {
		Biome vanillaBiomes[] = super.getBiomeMap(biomes, cubeX, cubeZ, width, length);
		return this.convertToCubeBiomes(vanillaBiomes);
	}
	
	/**
	 * Return a list of biomes for the specified blocks. Args: listToReuse, x, y, width, length, cacheFlag (if false, don't check biomeCache to avoid infinite loop in BiomeCacheBlock)
	 */
	@Override
	public Biome[] getBiomeGenAt(Biome[] biomes, int cubeX, int cubeZ, int width, int length, boolean flag) {
		Biome vanillaBiomes[] = super.getBiomeGenAt(biomes, cubeX, cubeZ, width, length, flag);
		return this.convertToCubeBiomes(vanillaBiomes);
	}
		private Biome[] convertToCubeBiomes( Biome[] array )
	{
		Biome[] cubeBiomes = new Biome[array.length];
		for( int i = 0; i < array.length; i++ )
			cubeBiomes[i] = CCBiome.getBiome( array[i].biomeID );
		return cubeBiomes;
	}
}
