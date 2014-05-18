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

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.gen.CubeGenerator;
import cuchaz.cubicChunks.gen.TestCubeGenerator;
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
//	private CubeGenerator m_generator;
	private TestCubeGenerator m_generator;
	private HashMap<Long,Column> m_loadedColumns;
	private BlankColumn m_blankColumn;
	private Deque<Long> m_cubesToUnload;
	
	private transient Set<Integer> m_values; // just temp space to save a new call on each chunk load
	
	public CubeProviderServer( WorldServer world )
	{
		super( world, null, null );
		
		m_worldServer = (CubeWorldServer)world;
		m_loader = new CubeLoader( world.getSaveHandler() );
//		m_generator = new CubeGenerator( world );
		m_generator = new TestCubeGenerator( world );
		m_loadedColumns = Maps.newHashMap();
		m_blankColumn = new BlankColumn( world, 0, 0 );
		m_cubesToUnload = new ArrayDeque<Long>();
		m_values = Sets.newHashSet();
	}
	
	@Override
	public boolean chunkExists( int cubeX, int cubeZ )
	{
		return m_loadedColumns.containsKey( AddressTools.getAddress( cubeX, cubeZ ) );
	}
	
	@Override
	public boolean cubeExists( int cubeX, int cubeY, int cubeZ )
	{
		// is the column loaded?
		long columnAddress = AddressTools.getAddress( cubeX, cubeZ );
		Column column = m_loadedColumns.get( columnAddress );
		if( column == null )
		{
			return false;
		}
		
		// is the cube loaded?
		return column.getCube( cubeY ) != null;
	}
	
	@Override
	public Column loadChunk( int cubeX, int cubeZ )
	{
		// find out the cubes in this column near any players or spawn points
		m_values.clear();
		getActiveCubeAddresses( m_values, cubeX, cubeZ );
		
		// actually load those cubes
		for( long address : m_values )
		{
			loadCube( cubeX, AddressTools.getY( address ), cubeZ );
		}
		
		Column column = provideChunk( cubeX, cubeZ );
		
		// remove the column's cubes from the unload queue
		for( Cube cube : column.cubes() )
		{
			m_cubesToUnload.remove( cube.getAddress() );
		}
		
		return column;
	}
	
	@Override
	public Column provideChunk( int cubeX, int cubeZ )
	{
		// check for the column
		Column column = m_loadedColumns.get( AddressTools.getAddress( cubeX, cubeZ ) );
		if( column != null )
		{
			return column;
		}
		
		// load the column or send back a proxy
		if( m_worldServer.findingSpawnPoint )
		{
			// if we're finding a spawn point, we only need to load the cubes above sea level
			return loadCubesAboveSeaLevel( cubeX, cubeZ );
		}
		else
		{
			return m_blankColumn;
		}
	}
	
	private Column loadCubesAboveSeaLevel( int cubeX, int cubeZ )
	{
		// UNDONE: do something smarter about the sea level
		final int SeaLevel = 63;
		
		int i;
		// load the cube at sea level
		// keep loading the next cube up until we don't get anything back or we 
		// hit the limit of 16 cubes above sea level. This will prevent the endless 
		// generation issues from freezing the game during initial world generation.
		Column column = null;
		for( int cubeY=Coords.blockToCube( SeaLevel ); cubeY < 16; cubeY++ )
		{
			Cube cube = loadCube( cubeX, cubeY, cubeZ );
			if( !cube.isEmpty() )
			{
				column = cube.getColumn();
			}
			else
			{
				break;
			}
		}
		
		return column;
	}
	
	public Cube loadCubeAndNeighbors( int cubeX, int cubeY, int cubeZ )
	{
		// load the requested cube
		Cube cube = loadCube( cubeX, cubeY, cubeZ );
		
		// load the neighbors
		loadCube( cubeX - 1, cubeY - 1, cubeZ - 1 );
		loadCube( cubeX - 1, cubeY - 1, cubeZ + 0 );
		loadCube( cubeX - 1, cubeY - 1, cubeZ + 1 );
		loadCube( cubeX + 0, cubeY - 1, cubeZ - 1 );
		loadCube( cubeX + 0, cubeY - 1, cubeZ + 0 );
		loadCube( cubeX + 0, cubeY - 1, cubeZ + 1 );
		loadCube( cubeX + 1, cubeY - 1, cubeZ - 1 );
		loadCube( cubeX + 1, cubeY - 1, cubeZ + 0 );
		loadCube( cubeX + 1, cubeY - 1, cubeZ + 1 );
		
		loadCube( cubeX - 1, cubeY + 0, cubeZ - 1 );
		loadCube( cubeX - 1, cubeY + 0, cubeZ + 0 );
		loadCube( cubeX - 1, cubeY + 0, cubeZ + 1 );
		loadCube( cubeX + 0, cubeY + 0, cubeZ - 1 );
		loadCube( cubeX + 0, cubeY + 0, cubeZ + 1 );
		loadCube( cubeX + 1, cubeY + 0, cubeZ - 1 );
		loadCube( cubeX + 1, cubeY + 0, cubeZ + 0 );
		loadCube( cubeX + 1, cubeY + 0, cubeZ + 1 );
		
		loadCube( cubeX - 1, cubeY + 1, cubeZ - 1 );
		loadCube( cubeX - 1, cubeY + 1, cubeZ + 0 );
		loadCube( cubeX - 1, cubeY + 1, cubeZ + 1 );
		loadCube( cubeX + 0, cubeY + 1, cubeZ - 1 );
		loadCube( cubeX + 0, cubeY + 1, cubeZ + 0 );
		loadCube( cubeX + 0, cubeY + 1, cubeZ + 1 );
		loadCube( cubeX + 1, cubeY + 1, cubeZ - 1 );
		loadCube( cubeX + 1, cubeY + 1, cubeZ + 0 );
		loadCube( cubeX + 1, cubeY + 1, cubeZ + 1 );
		
		return cube;
	}
	
	@Override
	public Cube loadCube( int cubeX, int cubeY, int cubeZ )
	{
		long cubeAddress = AddressTools.getAddress( cubeX, cubeY, cubeZ );
		long columnAddress = AddressTools.getAddress( cubeX, cubeZ );
		
		// step 1: get a column
		
		// is the column already loaded?
		Column column = m_loadedColumns.get( columnAddress );
		if( column == null )
		{
			// try loading it
			try
			{
				column = m_loader.loadColumn( m_worldServer, cubeX, cubeZ );
			}
			catch( IOException ex )
			{
				log.error( String.format( "Unable to load column (%d,%d)", cubeX, cubeZ ), ex );
				return null;
			}
			
			if( column == null )
			{
				// there wasn't a column, generate a new one
				column = m_generator.generateColumn( cubeX, cubeZ );
			}
			else
			{
				// the column was loaded
				column.lastSaveTime = m_worldServer.getTotalWorldTime();
			}
		}
		assert( column != null );
		
		// step 2: get a cube
		
		// is the cube already loaded?
		Cube cube = column.getCube( cubeY );
		if( cube != null )
		{
			return cube;
		}
		
		// try to load the cube
		try
		{
			cube = m_loader.loadCubeAndAddToColumn( m_worldServer, column, cubeAddress );
		}
		catch( IOException ex )
		{
			log.error( String.format( "Unable to load cube (%d,%d,%d)", cubeX, cubeY, cubeZ ), ex );
			return null;
		}
		
		if( cube == null )
		{
			// generate a new cube
			cube = m_generator.generateCube( column, cubeX, cubeY, cubeZ );
			
			// NOTE: have to do generator population after the cube is lit
			// UNDONE: make a chunk population queue
			//m_generator.populate( m_generator, cubeX, cubeY, cubeZ );
		}
		
		assert( cube != null );
		
		// add the column to the cache
		m_loadedColumns.put( columnAddress, column );
		
		// init the column
		column.onChunkLoad();
		column.isTerrainPopulated = true;
		column.resetPrecipitationHeight();
		
		// relight the cube
		cube.setIsLit( false );
		m_worldServer.getLightingManager().queueSkyLightCalculation( columnAddress );
		m_worldServer.getLightingManager().queueFirstLightCalculation( cubeAddress );
		
		// init the cube
		cube.onLoad();
		
		return cube;
	}
	
	@Override
	public void unloadChunksIfNotNearSpawn( int cubeX, int cubeZ )
	{
		throw new UnsupportedOperationException();
	}
	
	public void unloadCubeIfNotNearSpawn( Cube cube )
	{
		// NOTE: this is the main unload method for block data!
		
		unloadCubeIfNotNearSpawn( cube.getX(), cube.getY(), cube.getZ() );
	}
	
	public void unloadCubeIfNotNearSpawn( int cubeX, int cubeY, int cubeZ )
	{
		// don't unload cubes near the spawn
		if( cubeIsNearSpawn( cubeX, cubeY, cubeZ ) )
		{
			return;
		}
		
		// queue the cube for unloading
		m_cubesToUnload.add( AddressTools.getAddress( cubeX, cubeY, cubeZ ) );
	}
	
	@Override
	public void unloadAllChunks( )
	{
		// unload all the cubes in the columns
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
		
		// unload cubes
		for( int i=0; i<MaxNumToUnload && !m_cubesToUnload.isEmpty(); i++ )
		{
			long cubeAddress = m_cubesToUnload.poll();
			long columnAddress = AddressTools.getAddress( AddressTools.getX( cubeAddress ), AddressTools.getZ( cubeAddress ) );
			
			// get the cube
			Column column = m_loadedColumns.get( columnAddress );
			if( column == null )
			{
				// already unloaded
				continue;
			}
			
			// unload the cube
			int cubeY = AddressTools.getY( cubeAddress );
			Cube cube = column.removeCube( cubeY );
			if( cube != null )
			{
				// tell the cube it has been unloaded
				cube.onUnload();
				
				// save the cube
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
			
			// save the cubes
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
		return "CubeProviderServer: " + m_loadedColumns.size() + " columns, Unload: " + m_cubesToUnload.size() + " cubes";
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
		return m_generator.getNearestStructure( world, structureType, blockX, blockY, blockZ );
	}
	
	@SuppressWarnings( "unchecked" )
	private void getActiveCubeAddresses( Collection<Integer> out, int cubeX, int cubeZ )
	{
		CubePlayerManager playerManager = (CubePlayerManager)m_worldServer.getPlayerManager();
		
		for( EntityPlayerMP player : (List<EntityPlayerMP>)m_worldServer.playerEntities )
		{
			// check for cubes near players
			for( Long address : playerManager.getVisibleCubeAddresses( player ) )
			{
				int visibleCubeX = AddressTools.getX( address );
				int visibleCubeZ = AddressTools.getZ( address );
				if( visibleCubeX == cubeX && visibleCubeZ == cubeZ )
				{
					out.add( AddressTools.getY( address ) );
				}
			}
			
			// or near their spawn point
			if( player.getBedLocation() != null )
			{
				int spawnCubeX = Coords.blockToCube( player.getBedLocation().posX );
				int spawnCubeY = Coords.blockToCube( player.getBedLocation().posY );
				int spawnCubeZ = Coords.blockToCube( player.getBedLocation().posZ );
				if( spawnCubeX == cubeX && spawnCubeZ == cubeZ )
				{
					for( int y=-PlayerSpawnChunkDistance; y<=PlayerSpawnChunkDistance; y++ )
					{
						out.add( spawnCubeY + y );
					}
				}
			}
		}
		
		// or near world spawns
		if( m_worldServer.getSpawnPoint() != null )
		{
			int spawnCubeX = Coords.blockToCube( m_worldServer.getSpawnPoint().posX );
			int spawnCubeY = Coords.blockToCube( m_worldServer.getSpawnPoint().posY );
			int spawnCubeZ = Coords.blockToCube( m_worldServer.getSpawnPoint().posZ );
			int dx = Math.abs( spawnCubeX - cubeX );
			int dz = Math.abs( spawnCubeZ - cubeZ );
			if( dx <= WorldSpawnChunkDistance && dz <= WorldSpawnChunkDistance )
			{
				for( int y=-WorldSpawnChunkDistance; y<=WorldSpawnChunkDistance; y++ )
				{
					out.add( spawnCubeY + y );
				}
			}
		}
	}
	
	private boolean cubeIsNearSpawn( int cubeX, int cubeY, int cubeZ )
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
		int dx = Math.abs( spawnX - cubeX );
		int dy = Math.abs( spawnY - cubeY );
		int dz = Math.abs( spawnZ - cubeZ );
		return dx <= WorldSpawnChunkDistance && dy <= WorldSpawnChunkDistance && dz <= WorldSpawnChunkDistance;
	}
}
