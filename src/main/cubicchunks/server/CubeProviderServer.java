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
 *******************************************************************************/
package cubicchunks.server;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.entity.EnumCreatureType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

<<<<<<< HEAD:src/main/java/cubicchunks/server/CubeProviderServer.java
<<<<<<< HEAD:src/cuchaz/cubicChunks/server/CubeProviderServer.java
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.generator.ColumnGenerator;
import cuchaz.cubicChunks.generator.GeneratorStage;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.world.BlankColumn;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

=======
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/server/CubeProviderServer.java
=======
import cubicchunks.CubeProvider;
import cubicchunks.generator.ColumnGenerator;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;

>>>>>>> 69175bb... - Refactored package structure again to remove /java/:src/main/cubicchunks/server/CubeProviderServer.java
public class CubeProviderServer extends ChunkProviderServer implements CubeProvider
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int WorldSpawnChunkDistance = 12;
	
	private CubeWorldServer m_worldServer;
	private CubeLoader m_loader;
	private ColumnGenerator m_columnGenerator;
	private HashMap<Long,Column> m_loadedColumns;
	private BlankColumn m_blankColumn;
	private Deque<Long> m_cubesToUnload;
	
	public CubeProviderServer( CubeWorldServer world )
	{
		super( world, null, null );
		
		m_worldServer = (CubeWorldServer)world;
		m_loader = new CubeLoader( world.getSaveHandler() );
		m_columnGenerator = new ColumnGenerator( world );
		m_loadedColumns = Maps.newHashMap();
		m_blankColumn = new BlankColumn( world, 0, 0 );
		m_cubesToUnload = new ArrayDeque<Long>();
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
		// in the tall worlds scheme, load and provide columns/chunks are semantically the same thing
		// but load/provide cube do actually do different things
		return provideChunk( cubeX, cubeZ );
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
		
		return m_blankColumn;
	}
	
	@Override
	public Cube provideCube( int cubeX, int cubeY, int cubeZ )
	{
		// is the column loaded?
		long columnAddress = AddressTools.getAddress( cubeX, cubeZ );
		Column column = m_loadedColumns.get( columnAddress );
		if( column == null )
		{
			return null;
		}
		
		return column.getCube( cubeY );
	}
	
	public void loadCubeAndNeighbors( int cubeX, int cubeY, int cubeZ )
	{
		// load the requested cube
		loadCube( cubeX, cubeY, cubeZ );
		
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
	}
	
	public void loadCube( int cubeX, int cubeY, int cubeZ )
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
				return;
			}
			
			if( column == null )
			{
				// there wasn't a column, generate a new one
				column = m_columnGenerator.generateColumn( cubeX, cubeZ );
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
			return;
		}
		
		// try to load the cube
		try
		{
			cube = m_loader.loadCubeAndAddToColumn( m_worldServer, column, cubeAddress );
		}
		catch( IOException ex )
		{
			log.error( String.format( "Unable to load cube (%d,%d,%d)", cubeX, cubeY, cubeZ ), ex );
			return;
		}
		
		if( cube == null )
		{
			// start the cube generation process with an empty cube
			cube = column.getOrCreateCube( cubeY, true );
			cube.setGeneratorStage( GeneratorStage.getFirstStage() );
		}
		
		if( !cube.getGeneratorStage().isLastStage() )
		{
			// queue the cube to finish generation
			m_worldServer.getGeneratorPipeline().generate( cube );
		}
		else
		{
			// queue the cube for re-lighting
			m_worldServer.getLightingManager().queueFirstLightCalculation( cubeAddress );
		}
		
		// add the column to the cache
		m_loadedColumns.put( columnAddress, column );
		
		// init the column
		if( !column.isChunkLoaded )
		{
			column.onChunkLoad();
		}
		column.isTerrainPopulated = true;
		column.resetPrecipitationHeight();
		
		// init the cube
		cube.onLoad();
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
				column.onChunkUnload();
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
		// UNDONE: ask one of the pipeline processors for these things
		// This call only does the continuous spawning of mobs. 
		CubeBiomeGenBase var5 = (CubeBiomeGenBase) m_worldServer.getBiomeGenForCoords( blockX >> 4, blockZ >> 4 );
		return  var5.getSpawnableList( creatureType );
		
		//return null;
	}
	
	@Override
	public ChunkPosition func_147416_a( World world, String structureType, int blockX, int blockY, int blockZ )
	{
		/* UNDONE: ask the one of the pipeline processors for these things
		if( "Stronghold".equals( structureType ) && m_strongholdGenerator != null )
		{
			return m_strongholdGenerator.func_151545_a( world, blockX, blockY, blockZ );
		}
		*/
		return null;
	}
	
	public void recreateStructures( int cubeX, int cubeY, int cubeZ )
	{
		/* UNDONE: ask the one of the pipeline processors to do this
		if( m_mapFeaturesEnabled )
		{
			m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
			m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
			m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
			m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
		}
		*/
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
