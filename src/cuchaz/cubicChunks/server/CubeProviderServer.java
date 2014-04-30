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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.cubicChunks.CubeGenerator;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.BlankColumn;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public class CubeProviderServer extends ChunkProviderServer implements CubeProvider
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int WorldSpawnChunkDistance = 12;
	private static final int PlayerSpawnChunkDistance = 1;
	
	private CubeWorldServer m_worldServer;
	private CubeLoader m_loader;
	private CubeGenerator m_generator;
	private HashMap<Long,Column> m_loadedColumns;
	private BlankColumn m_blankColumn;
	private Deque<Long> m_cubesToUnload;
	
	private transient Set<Integer> m_values; // just temp space to save a new call on each chunk load
	
	public CubeProviderServer( WorldServer world )
	{
		super( world, null, null );
		
		m_worldServer = (CubeWorldServer)world;
		m_loader = new CubeLoader( world.getSaveHandler() );
		m_generator = new CubeGenerator( world );
		m_loadedColumns = Maps.newHashMap();
		m_blankColumn = new BlankColumn( world, 0, 0 );
		m_cubesToUnload = new ArrayDeque<Long>();
		m_values = Sets.newHashSet();
	}
	
	@Override
	public boolean chunkExists( int chunkX, int chunkZ )
	{
		return m_loadedColumns.containsKey( AddressTools.getAddress( chunkX, chunkZ ) );
	}
	
	@Override
	public boolean cubeExists( int chunkX, int chunkY, int chunkZ )
	{
		// check the column first
		if( !chunkExists( chunkX, chunkZ ) )
		{
			return false;
		}
		
		// then check the cubic chunk
		return provideChunk( chunkX, chunkZ ).getCube( chunkY ) != null;
	}
	
	@Override
	public Column loadChunk( int chunkX, int chunkZ )
	{
		// find out the cubic chunks in this column near any players or spawn points
		m_values.clear();
		getActiveCubeAddresses( m_values, chunkX, chunkZ );
		
		// TEMP
		if( m_values.isEmpty() )
		{
			System.out.println( String.format( "Tried to load column (%d,%d) but nothing was nearby, spawn=(%d,%d,%d)",
				chunkX, chunkZ,
				m_worldServer.getSpawnPoint().posX, m_worldServer.getSpawnPoint().posY, m_worldServer.getSpawnPoint().posZ
			) );
			Thread.dumpStack();
			return null;
		}
		
		// actually load those cubic chunks
		for( long address : m_values )
		{
			loadCube( chunkX, AddressTools.getY( address ), chunkZ );
		}
		
		Column column = provideChunk( chunkX, chunkZ );
		
		// remove the column's cubic chunks from the unload queue
		for( Cube cube : column.cubes() )
		{
			m_cubesToUnload.remove( cube.getAddress() );
		}
		
		return column;
	}
	
	@Override
	public Column provideChunk( int chunkX, int chunkZ )
	{
		// check for the column
		Column column = m_loadedColumns.get( AddressTools.getAddress( chunkX, chunkZ ) );
		if( column != null )
		{
			// TEMP
			if( !column.hasCubes() )
			{
				System.out.println( String.format( "provided column (%d,%d) has no cubic chunks!", chunkX, chunkZ ) );
			}
			
			return column;
		}
		
		// load the column or send back a proxy
		if( m_worldServer.findingSpawnPoint )
		{
			// if we're finding a spawn point, we only need to load the cubic chunks above sea level
			return loadCubesAboveSeaLevel( chunkX, chunkZ );
		}
		else
		{
			return m_blankColumn;			
		}
	}
	
	private Column loadCubesAboveSeaLevel( int chunkX, int chunkZ )
	{
		// UNDONE: do something smarter about the sea level
		final int SeaLevel = 63;
		
		Column column = loadChunk( chunkX, chunkZ );
		
		int minChunkY = Coords.blockToChunk( SeaLevel );
		int maxChunkY = column.getTopCubeY();
		for( int chunkY=minChunkY; chunkY<=maxChunkY; chunkY++ )
		{
			loadCube( chunkX, chunkY, chunkZ );
		}
		
		return column;
	}

	@Override
	public Cube loadCube( int chunkX, int chunkY, int chunkZ )
	{
		long cubeAddress = AddressTools.getAddress( chunkX, chunkY, chunkZ );
		long columnAddress = AddressTools.getAddress( chunkX, chunkZ );
		
		// is the column loaded?
		Column column = m_loadedColumns.get( columnAddress );
		if( column != null )
		{
			// is the cubic chunk loaded?
			Cube cube = column.getCube( chunkY );
			if( cube != null )
			{
				return cube;
			}
			else
			{
				return loadCubeIntoColumn( column, cubeAddress );
			}
		}
		else
		{
			try
			{
				// at this point, column and cubic chunk loading are intertwined
				// so try to load the column and the cubic chunk at the same time
				Cube cube;
				column = m_loader.loadColumn( m_worldServer, chunkX, chunkZ );
				if( column == null )
				{
					// there wasn't a column, generate a new one
					column = m_generator.provideChunk( chunkX, chunkZ );
					
					// was the cubic chunk generated?
					cube = column.getCube( chunkY );
					if( cube != null )
					{
						// tell the cubic chunk it was loaded
						cube.onLoad();
					}
				}
				else
				{
					// the column was loaded
					column.lastSaveTime = m_worldServer.getTotalWorldTime();
					
					// load the cubic chunk too
					cube = loadCubeIntoColumn( column, cubeAddress );
				}
				
				// if we have a valid cubic chunk, init the column
				if( cube != null )
				{
					// add the column to the cache
					m_loadedColumns.put( columnAddress, column );
					
					// init the column
					column.onChunkLoad();
					column.populateChunk( this, this, chunkX, chunkZ );
				}
				
				return cube;
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to load column (%d,,%d)", chunkX, chunkZ ), ex );
				return null;
			}
		}
	}
	
	private Cube loadCubeIntoColumn( Column column, long address )
	{
		try
		{
			// load the cubic chunk
			Cube cube = m_loader.loadCubeAndAddToColumn( m_worldServer, column, address );
			
			if( cube == null )
			{
				// cubic chunk does not exist
				return null;
			}
			
			// tell the cubic chunk it was loaded
			cube.onLoad();
			
			return cube;
		}
		catch( IOException ex )
		{
			log.error( String.format( "Unable to load cubic chunk (%d,%d,%d)",
				AddressTools.getX( address ), AddressTools.getY( address ), AddressTools.getZ( address )
			), ex );
			return null;
		}
	}

	@Override
	public void unloadChunksIfNotNearSpawn( int chunkX, int chunkZ )
	{
		throw new UnsupportedOperationException();
	}
	
	public void unloadCubeIfNotNearSpawn( Cube cube )
	{
		// NOTE: this is the main unload method for block data!
		
		unloadCubeIfNotNearSpawn( cube.getX(), cube.getY(), cube.getZ() );
	}
	
	public void unloadCubeIfNotNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		// don't unload cubic chunks near the spawn
		if( cubeIsNearSpawn( chunkX, chunkY, chunkZ ) )
		{
			return;
		}
		
		// queue the cubic chunk for unloading
		m_cubesToUnload.add( AddressTools.getAddress( chunkX, chunkY, chunkZ ) );
	}
	
	@Override
	public void unloadAllChunks( )
	{
		// unload all the cubic chunks in the columns
		for( Column column : m_loadedColumns.values() )
		{
			for( Cube cube : column.cubes() )
			{
				m_cubesToUnload.add( cube.getAddress() );
			}
		}
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
		
		final int MaxNumToUnload = 400;
		
		// unload cubic chunks
		for( int i=0; i<MaxNumToUnload && !m_cubesToUnload.isEmpty(); i++ )
		{
			long cubeAddress = m_cubesToUnload.poll();
			long columnAddress = AddressTools.getAddress( AddressTools.getX( cubeAddress ), AddressTools.getZ( cubeAddress ) );
			
			// get the cubic chunk
			Column column = m_loadedColumns.get( columnAddress );
			if( column == null )
			{
				// already unloaded
				continue;
			}
			
			// unload the cubic chunk
			int chunkY = AddressTools.getY( cubeAddress );
			Cube cube = column.removeCube( chunkY );
			if( cube != null )
			{
				// tell the cubic chunk it has been unloaded
				cube.onUnload();
				
				// save the cubic chunk
				m_loader.saveCube( cube );
			}
			
			// unload empty columns
			if( !column.hasCubes() )
			{
				column.onChunkLoad();
				m_loadedColumns.remove( columnAddress );
				m_loader.saveColumn( column );
			}
		}
		
		return false;
	}
	
	@Override
	public boolean saveChunks( boolean alwaysTrue, IProgressUpdate progress )
	{
		for( Column column : m_loadedColumns.values() )
		{
			// save the column
			if( column.needsSaving( alwaysTrue ) )
			{
				m_loader.saveColumn( column );
			}
			
			// save the cubic chunks
			for( Cube cube : column.cubes() )
			{
				if( cube.needsSaving() )
				{
					m_loader.saveCube( cube );
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String makeString( )
	{
		return "CubeProviderServer: " + m_loadedColumns.size() + " columns, Unload: " + m_cubesToUnload.size() + " cubic chunks";
	}
	
	@Override
	public int getLoadedChunkCount( )
	{
		return m_loadedColumns.size();
	}
	
	@Override
	@SuppressWarnings( "rawtypes" )
	public List getPossibleCreatures( EnumCreatureType creatureType, int blockX, int blockY, int blockZ )
	{
		return m_generator.getPossibleCreatures( creatureType, blockX, blockY, blockZ );
	}
	
	@Override
	public ChunkPosition func_147416_a( World world, String structureType, int blockX, int blockY, int blockZ )
	{
		return m_generator.func_147416_a( world, structureType, blockX, blockY, blockZ );
	}
	
	@SuppressWarnings( "unchecked" )
	private void getActiveCubeAddresses( Collection<Integer> out, int chunkX, int chunkZ )
	{
		CubePlayerManager playerManager = (CubePlayerManager)m_worldServer.getPlayerManager();
		
		for( EntityPlayerMP player : (List<EntityPlayerMP>)m_worldServer.playerEntities )
		{
			// check for cubic chunks near players
			for( Long address : playerManager.getVisibleCubeAddresses( player ) )
			{
				int visibleChunkX = AddressTools.getX( address );
				int visibleChunkZ = AddressTools.getZ( address );
				if( visibleChunkX == chunkX && visibleChunkZ == chunkZ )
				{
					out.add( AddressTools.getY( address ) );
				}
			}
			
			// or near their spawn point
			if( player.getBedLocation() != null )
			{
				int spawnChunkX = Coords.blockToChunk( player.getBedLocation().posX );
				int spawnChunkY = Coords.blockToChunk( player.getBedLocation().posY );
				int spawnChunkZ = Coords.blockToChunk( player.getBedLocation().posZ );
				if( spawnChunkX == chunkX && spawnChunkZ == chunkZ )
				{
					for( int y=-PlayerSpawnChunkDistance; y<=PlayerSpawnChunkDistance; y++ )
					{
						out.add( spawnChunkY + y );
					}
				}
			}
		}
		
		// or near world spawns
		if( m_worldServer.getSpawnPoint() != null )
		{
			int spawnChunkX = Coords.blockToChunk( m_worldServer.getSpawnPoint().posX );
			int spawnChunkY = Coords.blockToChunk( m_worldServer.getSpawnPoint().posY );
			int spawnChunkZ = Coords.blockToChunk( m_worldServer.getSpawnPoint().posZ );
			int dx = Math.abs( spawnChunkX - chunkX );
			int dz = Math.abs( spawnChunkZ - chunkZ );
			if( dx <= WorldSpawnChunkDistance && dz <= WorldSpawnChunkDistance )
			{
				for( int y=-WorldSpawnChunkDistance; y<=WorldSpawnChunkDistance; y++ )
				{
					out.add( spawnChunkY + y );
				}
			}
		}
	}
	
	private boolean cubeIsNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		if( !m_worldServer.provider.canRespawnHere() )
		{
			// no spawn points
			return false;
		}
		
		long address = m_worldServer.getSpawnPointCubeAddress();
		int spawnX = AddressTools.getX( address );
		int spawnY = AddressTools.getY( address );
		int spawnZ = AddressTools.getZ( address );
		int dx = Math.abs( spawnX - chunkX );
		int dy = Math.abs( spawnY - chunkY );
		int dz = Math.abs( spawnZ - chunkZ );
		return dx <= WorldSpawnChunkDistance && dy <= WorldSpawnChunkDistance && dz <= WorldSpawnChunkDistance;
	}
}
