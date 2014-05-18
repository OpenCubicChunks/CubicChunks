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
package cuchaz.cubicChunks.server;

import java.util.List;
import java.util.Set;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.accessors.WorldServerAccessor;
import cuchaz.cubicChunks.gen.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.gen.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import cuchaz.cubicChunks.lighting.LightingManager;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;

public class CubeWorldServer extends WorldServer implements CubeWorld
{
	private LightingManager m_lightingManager;
	
	public CubeWorldServer( MinecraftServer server, ISaveHandler saveHandler, String worldName, int dimension, WorldSettings settings, Profiler profiler )
	{
		super( server, saveHandler, worldName, dimension, settings, profiler );
		
		// set the player manager
		CubePlayerManager playerManager = new CubePlayerManager( this, server.getConfigurationManager().getViewDistance() );
		WorldServerAccessor.setPlayerManager( this, playerManager );
	}
	
	@Override
	protected IChunkProvider createChunkProvider()
    {
		CubeProviderServer chunkProvider = new CubeProviderServer( this );
		WorldServerAccessor.setChunkProvider( this, chunkProvider );
		
		// init the lighting manager
		m_lightingManager = new LightingManager( this, chunkProvider );
		
		return chunkProvider;
    }
	
	@Override
	public CubeProvider getCubeProvider( )
	{
		return (CubeProvider)chunkProvider;
	}
	
	@Override
	public LightingManager getLightingManager( )
	{
		return m_lightingManager;
	}
	
	@Override
	public void tick( )
	{
		super.tick();
		
		theProfiler.startSection( "lightingEngine" );
		m_lightingManager.tick();
		theProfiler.endSection();
	}
	
    /**
     * only spawns creatures allowed by the chunkProvider
     */
	@Override
    public net.minecraft.world.biome.BiomeGenBase.SpawnListEntry spawnRandomCreature(EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4)
    {
//        List var5 = (List) this.getChunkProvider().getPossibleCreatures(par1EnumCreatureType, par2, par3, par4);
//        return var5 != null && !var5.isEmpty() ? (net.minecraft.world.biome.BiomeGenBase.SpawnListEntry)WeightedRandom.getRandomItem(this.rand, var5) : null;
		return null;
    }
	
	public long getSpawnPointCubeAddress( )
	{
		return AddressTools.getAddress(
			Coords.blockToCube( worldInfo.getSpawnX() ),
			Coords.blockToCube( worldInfo.getSpawnY() ),
			Coords.blockToCube( worldInfo.getSpawnZ() )
		);
	}
	
	@Override
	public boolean checkChunksExist( int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ )
	{
		return CubeProviderTools.blocksExist( (CubeProvider)chunkProvider, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ );
	}
	
	@Override
	public boolean updateLightByType( EnumSkyBlock lightType, int blockX, int blockY, int blockZ )
    {
		// forward to the new lighting system
		return m_lightingManager.computeDiffuseLighting( blockX, blockY, blockZ, lightType );
    }
	
	@Override //            tick
	@SuppressWarnings( "unchecked" )
	protected void func_147456_g( )
	{
		super.func_147456_g();
		
		// apply random ticks
		for( ChunkCoordIntPair coords : (Set<ChunkCoordIntPair>)activeChunkSet )
		{
			Column column = (Column)chunkProvider.provideChunk( coords.chunkXPos, coords.chunkZPos );
			column.doRandomTicks();
		}
	}
}
