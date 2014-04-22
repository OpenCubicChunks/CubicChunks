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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CubicChunkProviderServer extends ChunkProviderServer implements CubicChunkProvider
{
	private static final Logger log = LogManager.getLogger();
	
	private WorldServer m_worldServer;
	private CubicChunkLoader m_loader;
	private CubicChunkGenerator m_generator;
	private LongHashMap m_loadedColumns;
	private BlankColumn m_blankColumn;
	private List<Long> m_columnsToUnload;
	
	public CubicChunkProviderServer( WorldServer world )
	{
		super( null, null, null );
		
		m_worldServer = world;
		m_loader = new CubicChunkLoader( world.getSaveHandler() );
		m_generator = new CubicChunkGenerator( world );
		m_loadedColumns = new LongHashMap();
		m_blankColumn = new BlankColumn( world, 0, 0 );
		m_columnsToUnload = new ArrayList<Long>();
	}
	
	@Override
	public boolean chunkExists( int chunkX, int chunkZ )
	{
		return m_loadedColumns.containsItem( getColumnAddress( chunkX, chunkZ ) );
	}
	
	@Override
	public boolean cubicChunkExists( int chunkX, int chunkY, int chunkZ )
	{
		// check the column first
		if( !chunkExists( chunkX, chunkZ ) )
		{
			return false;
		}
		
		// then check the cubic chunk
		return provideChunk( chunkX, chunkZ ).getCubicChunk( chunkY ) != null;
	}
	
	@Override
	public Column loadChunk( int chunkX, int chunkZ )
	{
		long address = getColumnAddress( chunkX, chunkZ );
		
		// remove this column from the unload queue
		m_columnsToUnload.remove( address );
		
		// is this column already loaded?
		Column column = (Column)m_loadedColumns.getValueByKey( address );
		if( column != null )
		{
			return column;
		}
		
		// try to load the column from storage
		try
		{
			column = m_loader.loadColumn( m_worldServer, chunkX, chunkZ );
			if( column != null )
			{
				column.lastSaveTime = m_worldServer.getTotalWorldTime();
			}
			else
			{
				// generate a new column
				column = m_generator.provideChunk( chunkX, chunkZ );
			}
			
			// add the column to the cache
			m_loadedColumns.add( address, column );
			
			// init the column
			column.onChunkLoad();
			column.populateChunk( this, this, chunkX, chunkZ );
			
			return column;
		}
		catch( IOException ex )
		{
			// something bad happened, just return a blank column
			log.error( String.format( "Unable to load column (%d,%d)", chunkX, chunkZ ), ex );
			return m_blankColumn;
		}
	}
	
	@Override
	public Column provideChunk( int chunkX, int chunkZ )
	{
		// check for the column
		Column column = (Column)m_loadedColumns.getValueByKey( getColumnAddress( chunkX, chunkZ ) );
		if( column != null )
		{
			return column;
		}
		
		// load the column or send back a proxy
		if( m_worldServer.findingSpawnPoint || loadChunkOnProvideRequest )
		{
			return loadChunk( chunkX, chunkZ );
		}
		else
		{
			return m_blankColumn;			
		}
	}
	
	@Override
	public CubicChunk loadCubicChunk( int chunkX, int chunkY, int chunkZ )
	{
		// UNDONE: re-evaluate this method!
		
		// load the column
		Column column = loadChunk( chunkX, chunkZ );
		
		// check for the cubic chunk
		CubicChunk cubicChunk = column.getCubicChunk( chunkY );
		if( cubicChunk == null )
		{
			// UNDONE: try to load the cubic chunk
		}
		
		return cubicChunk;
	}
	
	@Override
	public void unloadChunksIfNotNearSpawn( int chunkX, int chunkZ )
	{
		if( m_worldServer.provider.canRespawnHere() )
		{
			ChunkCoordinates coords = m_worldServer.getSpawnPoint();
			int blockX = chunkX * 16 + 8 - coords.posX;
			int blockZ = chunkZ * 16 + 8 - coords.posZ;
			short blockDistance = 128; // 8 chunks
			
			if( blockX < -blockDistance || blockX > blockDistance || blockZ < -blockDistance || blockZ > blockDistance )
			{
				this.chunksToUnload.add( Long.valueOf( ChunkCoordIntPair.chunkXZ2Int( chunkX, chunkZ ) ) );
			}
		}
		else
		{
			this.chunksToUnload.add( Long.valueOf( ChunkCoordIntPair.chunkXZ2Int( chunkX, chunkZ ) ) );
		}
	}
	
	public void unloadCubicChunkIfNotNearSpawn( CubicChunk cubicChunk )
	{
		unloadCubicChunkIfNotNearSpawn( cubicChunk.getX(), cubicChunk.getY(), cubicChunk.getZ() );
	}
	
	public void unloadCubicChunkIfNotNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		// UNDONE
	}
	
	@Override
	public void unloadAllChunks( )
	{
		for( Column column : m_loadedColumns )
		{
			
		}
	}
	
	@Override
	public boolean unloadQueuedChunks( )
	{
		if( !this.worldObj.levelSaving )
		{
			for( int var1 = 0; var1 < 100; ++var1 )
			{
				if( !this.chunksToUnload.isEmpty() )
				{
					Long var2 = (Long)this.chunksToUnload.iterator().next();
					Chunk var3 = (Chunk)this.loadedChunkHashMap.getValueByKey( var2.longValue() );
					var3.onChunkUnload();
					this.safeSaveChunk( var3 );
					this.safeSaveExtraChunkData( var3 );
					this.chunksToUnload.remove( var2 );
					this.loadedChunkHashMap.remove( var2.longValue() );
					this.loadedChunks.remove( var3 );
				}
			}
			
			if( this.currentChunkLoader != null )
			{
				this.currentChunkLoader.chunkTick();
			}
		}
		
		return this.currentChunkProvider.unloadQueuedChunks();
	}
	
	@Override
	public boolean saveChunks( boolean flag, IProgressUpdate progress )
	{
		int var3 = 0;
		
		for( int var4 = 0; var4 < this.loadedChunks.size(); ++var4 )
		{
			Chunk var5 = (Chunk)this.loadedChunks.get( var4 );
			
			if( flag )
			{
				this.safeSaveExtraChunkData( var5 );
			}
			
			if( var5.needsSaving( flag ) )
			{
				this.safeSaveChunk( var5 );
				var5.isModified = false;
				++var3;
				
				if( var3 == 24 && !flag )
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String makeString( )
	{
		return "ServerChunkCache: " + this.loadedChunkHashMap.getNumHashElements() + " Drop: " + this.chunksToUnload.size();
	}
	
	@Override
	public int getLoadedChunkCount( )
	{
		return this.loadedChunkHashMap.getNumHashElements();
	}
	
	private long getColumnAddress( int chunkX, int chunkZ )
	{
		return AddressTools.getAddress( m_worldServer.provider.dimensionId, chunkX, 0, chunkZ );
	}
	
	private long getCubicChunkAddress( int chunkX, int chunkY, int chunkZ )
	{
		return AddressTools.getAddress( m_worldServer.provider.dimensionId, chunkX, chunkY, chunkZ );
	}
}
