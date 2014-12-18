/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.generator.biome;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;

public class WorldColumnManager extends WorldChunkManager
{
	private List<CubeBiomeGenBase> biomesToSpawnIn;
	
	protected WorldColumnManager( )
	{
		this.init();
	}
	
	public WorldColumnManager( long seed, WorldType worldType )
	{
		super( seed, worldType );
		this.init();
	}
	
	private void init( )
	{
		this.biomesToSpawnIn = new ArrayList<CubeBiomeGenBase>();
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forest );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.plains );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taiga );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taigaHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forestHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungle );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungleHills );
	}
	
	public WorldColumnManager( World world )
	{
		this( world.getSeed(), world.getWorldInfo().getTerrainType() );
	}
	
	@Override
	public List<CubeBiomeGenBase> getBiomesToSpawnIn( )
	{
		return this.biomesToSpawnIn;
	}
	
	@Override
	public CubeBiomeGenBase getBiomeGenAt( int xAbs, int zAbs )
	{
		return CubeBiomeGenBase.getBiome( super.getBiomeGenAt( xAbs, zAbs ).biomeID );
	}
	
	@Override
	public BiomeGenBase[] getBiomesForGeneration( BiomeGenBase[] biomes, int xGenBlock, int zGenBlock, int width, int length )
	{
		BiomeGenBase vanillaBiomes[] = super.getBiomesForGeneration( biomes, xGenBlock, zGenBlock, width, length );
		return this.convertToCubeBiomes( vanillaBiomes );
	}
	
	@Override
	public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length, boolean flag )
	{
		BiomeGenBase vanillaBiomes[] = super.getBiomeGenAt( biomes, blockX, blockZ, width, length, flag );
		return this.convertToCubeBiomes( vanillaBiomes );
	}
	
	private CubeBiomeGenBase[] convertToCubeBiomes( BiomeGenBase[] array )
	{
		CubeBiomeGenBase[] cubeBiomes = new CubeBiomeGenBase[array.length];
		for( int i = 0; i < array.length; i++ )
			cubeBiomes[i] = CubeBiomeGenBase.getBiome( array[i].biomeID );
		return cubeBiomes;
	}
}
