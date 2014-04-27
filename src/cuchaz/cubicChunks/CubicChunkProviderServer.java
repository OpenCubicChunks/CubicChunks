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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

public class CubicChunkProviderServer extends ChunkProviderServer implements CubicChunkProvider
{
	private static final Logger log = LogManager.getLogger();
	
	private CubicChunksWorldServer m_worldServer;
	private CubicChunkLoader m_loader;
	private CubicChunkGenerator m_generator;
	private HashMap<Long,Column> m_loadedColumns;
	private BlankColumn m_blankColumn;
	private Deque<Long> m_columnsToUnload;
	private Deque<Long> m_cubicChunksToUnload;
	
	public CubicChunkProviderServer( WorldServer world )
	{
		super( null, null, null );
		
		m_worldServer = (CubicChunksWorldServer)world;
		m_loader = new CubicChunkLoader( world.getSaveHandler() );
		m_generator = new CubicChunkGenerator( world );
		m_loadedColumns = Maps.newHashMap();
		m_blankColumn = new BlankColumn( world, 0, 0 );
		m_columnsToUnload = new ArrayDeque<Long>();
		m_cubicChunksToUnload = new ArrayDeque<Long>();
	}
	
	@Override
	public boolean chunkExists( int chunkX, int chunkZ )
	{
		return m_loadedColumns.containsKey( getColumnAddress( chunkX, chunkZ ) );
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
		// NEXTTIME: route this directly to provideChunk()?
		// but need to actually load things in special cases like:
		//    spawn area preparation
		//    player spawning (eg, at beds)
		// see list of loadChunk callers and figure out how to make those work...
		
		long address = getColumnAddress( chunkX, chunkZ );
		
		// remove this column from the unload queue
		m_columnsToUnload.remove( address );
		
		// is this column already loaded?
		Column column = (Column)m_loadedColumns.get( address );
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
			m_loadedColumns.put( address, column );
			
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
		Column column = (Column)m_loadedColumns.get( getColumnAddress( chunkX, chunkZ ) );
		if( column != null )
		{
			return column;
		}
		
		// load the column or send back a proxy
		if( m_worldServer.findingSpawnPoint )
		{
			// NOTE: if we're finding a spawn point, we only need to load the cubic chunks above sea level
			return loadCubicChunksAboveSeaLevel( chunkX, chunkZ );
		}
		else
		{
			return m_blankColumn;			
		}
	}
	
	@Override
	public CubicChunk loadCubicChunk( int chunkX, int chunkY, int chunkZ )
	{
		// NOTE: this is the main load method for block data!
		
		// get the column
		Column column = provideChunk( chunkX, chunkZ );
		
		// check for the cubic chunk
		CubicChunk cubicChunk = column.getCubicChunk( chunkY );
		if( cubicChunk != null )
		{
			return cubicChunk;
		}
		
		// UNDONE: load the cubic chunk and add it to the column
		return null;
	}
	
	@Override
	public void unloadChunksIfNotNearSpawn( int chunkX, int chunkZ )
	{
		throw new UnsupportedOperationException();
	}
	
	public void unloadCubicChunkIfNotNearSpawn( CubicChunk cubicChunk )
	{
		// NOTE: this is the main unload method for block data!
		
		unloadCubicChunkIfNotNearSpawn( cubicChunk.getX(), cubicChunk.getY(), cubicChunk.getZ() );
	}
	
	public void unloadCubicChunkIfNotNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		// don't unload cubic chunks near the spawn
		if( cubicChunkIsNearSpawn( chunkX, chunkY, chunkZ ) )
		{
			return;
		}
		
		// queue the cubic chunk for unloading
		m_cubicChunksToUnload.add( getCubicChunkAddress( chunkX, chunkY, chunkZ ) );
	}
	
	@Override
	public void unloadAllChunks( )
	{
		m_columnsToUnload.addAll( m_loadedColumns.keySet() );
	}
	
	@Override
	public boolean unloadQueuedChunks( )
	{
		// NOTE: the return value is completely ignored
		
		// don't unload if we're saving
		if( m_worldServer.levelSaving )
		{
			return false;
		}
		
		final int MaxNumToUnload = 100;
		
		// unload cubic chunks
		
		
		// unload columns
		for( int i=0; i<MaxNumToUnload && !m_columnsToUnload.isEmpty(); i++ )
		{
			long address = m_columnsToUnload.poll();
			
			// get the column
			Column column = (Column)m_loadedColumns.get( address );
			if( column == null )
			{
				// already unloaded
				continue;
			}
			
			// remove from the loaded set
			m_loadedColumns.remove( address );
			
			// tell the column is has been unloaded
			column.onChunkUnload();
			
			// save the column
			m_loader.saveColumn( m_worldServer, column );
		}
		
		return false;
	}
	
	@Override
	public boolean saveChunks( boolean alwaysTrue, IProgressUpdate progress )
	{
		for( Column column : m_loadedColumns.values() )
		{
			if( column.needsSaving( alwaysTrue ) )
			{
				// save the column
				m_loader.saveColumn( m_worldServer, column );
				column.isModified = false;
			}
		}
		
		return true;
	}
	
	@Override
	public String makeString( )
	{
		return "ServerChunkCache: " + m_loadedColumns.size() + " Drop: " + m_columnsToUnload.size();
	}
	
	@Override
	public int getLoadedChunkCount( )
	{
		return m_loadedColumns.size();
	}
	
	private long getColumnAddress( int chunkX, int chunkZ )
	{
		return AddressTools.getAddress( m_worldServer.provider.dimensionId, chunkX, 0, chunkZ );
	}
	
	private long getCubicChunkAddress( int chunkX, int chunkY, int chunkZ )
	{
		return AddressTools.getAddress( m_worldServer.provider.dimensionId, chunkX, chunkY, chunkZ );
	}
	
	private boolean cubicChunkIsNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		if( !m_worldServer.provider.canRespawnHere() )
		{
			// no spawn points
			return false;
		}
		
		final int MaxChunkDistance = 8;
		
		long address = m_worldServer.getSpawnPointCubicChunkAddress();
		int spawnX = AddressTools.getX( address );
		int spawnY = AddressTools.getY( address );
		int spawnZ = AddressTools.getZ( address );
		int dx = Math.abs( spawnX - chunkX );
		int dy = Math.abs( spawnY - chunkY );
		int dz = Math.abs( spawnZ - chunkZ );
		return dx <= MaxChunkDistance && dy <= MaxChunkDistance && dz <= MaxChunkDistance;
	}
}
