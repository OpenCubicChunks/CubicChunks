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

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.accessors.WorldServerAccessor;
import cuchaz.cubicChunks.lighting.LightingManager;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.Coords;

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
	
	public long getSpawnPointCubeAddress( )
	{
		return AddressTools.getAddress(
			Coords.blockToCube( worldInfo.getSpawnX() ),
			Coords.blockToCube( worldInfo.getSpawnY() ),
			Coords.blockToCube( worldInfo.getSpawnZ() )
		);
	}
	
	/* SOO much Minecraft code expects the bottom cubes to be loaded
	   Let's not tell Minecraft they're not actually loaded just yet
	@Override
	public boolean checkChunksExist( int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ )
	{
		// convert block bounds to chunk bounds
		int minCubeX = Coords.blockToChunk( minBlockX );
		int minCubeY = Coords.blockToChunk( minBlockY );
		int minCubeZ = Coords.blockToChunk( minBlockZ );
		int maxCubeX = Coords.blockToChunk( maxBlockX );
		int maxCubeY = Coords.blockToChunk( maxBlockY );
		int maxCubeZ = Coords.blockToChunk( maxBlockZ );
		
		// check for any missing cubes
		CubeProviderServer chunkProvider = (CubeProviderServer)WorldAccessor.getChunkProvider( this );
		for( int cubeX=minCubeX; cubeX<=maxCubeX; cubeX++ )
		{
			for( int cubeY=minCubeY; cubeY<=maxCubeY; cubeY++ )
			{
				for( int cubeZ=minCubeZ; cubeZ<=maxCubeZ; cubeZ++ )
				{
					if( !chunkProvider.cubeExists( cubeX, cubeY, cubeZ ) )
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	*/
	
	@Override
	public boolean updateLightByType( EnumSkyBlock lightType, int blockX, int blockY, int blockZ )
    {
		// forward to the new lighting system
		return m_lightingManager.computeDiffuseLighting( blockX, blockY, blockZ, lightType );
    }
}
