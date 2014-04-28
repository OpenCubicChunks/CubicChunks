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
package cuchaz.cubicChunks;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import cuchaz.cubicChunks.accessors.WorldServerAccessor;

public class CubicChunksWorldServer extends WorldServer
{
	public CubicChunksWorldServer( MinecraftServer server, ISaveHandler saveHandler, String worldName, int dimension, WorldSettings settings, Profiler profiler )
	{
		super( server, saveHandler, worldName, dimension, settings, profiler );
		
		// set the player manager
		CubicChunkPlayerManager playerManager = new CubicChunkPlayerManager( this, server.getConfigurationManager().getViewDistance() );
		WorldServerAccessor.setPlayerManager( this, playerManager );
	}
	
	@Override
	protected IChunkProvider createChunkProvider()
    {
		CubicChunkProviderServer chunkProvider = new CubicChunkProviderServer( this );
		WorldServerAccessor.setChunkProvider( this, chunkProvider );
		return chunkProvider;
    }
	
	public long getSpawnPointCubicChunkAddress( )
	{
		return AddressTools.getAddress(
			Coords.blockToChunk( worldInfo.getSpawnX() ),
			Coords.blockToChunk( worldInfo.getSpawnY() ),
			Coords.blockToChunk( worldInfo.getSpawnZ() )
		);
	}
	
	/* SOO much Minecraft code expects the bottom cubic chunks to be loaded
	   Let's not tell Minecraft they're not actually loaded just yet
	@Override
	public boolean checkChunksExist( int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ )
	{
		// convert block bounds to chunk bounds
		int minChunkX = Coords.blockToChunk( minBlockX );
		int minChunkY = Coords.blockToChunk( minBlockY );
		int minChunkZ = Coords.blockToChunk( minBlockZ );
		int maxChunkX = Coords.blockToChunk( maxBlockX );
		int maxChunkY = Coords.blockToChunk( maxBlockY );
		int maxChunkZ = Coords.blockToChunk( maxBlockZ );
		
		// check for any missing cubic chunks
		CubicChunkProviderServer chunkProvider = (CubicChunkProviderServer)WorldAccessor.getChunkProvider( this );
		for( int chunkX=minChunkX; chunkX<=maxChunkX; chunkX++ )
		{
			for( int chunkY=minChunkY; chunkY<=maxChunkY; chunkY++ )
			{
				for( int chunkZ=minChunkZ; chunkZ<=maxChunkZ; chunkZ++ )
				{
					if( !chunkProvider.cubicChunkExists( chunkX, chunkY, chunkZ ) )
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	*/
}
