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
package main.java.cubicchunks.client;

import main.java.cubicchunks.CubeProvider;
import main.java.cubicchunks.accessors.ChunkProviderClientAccessor;
import main.java.cubicchunks.generator.GeneratorStage;
import main.java.cubicchunks.world.BlankColumn;
import main.java.cubicchunks.world.Column;
import main.java.cubicchunks.world.Cube;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class CubeProviderClient extends ChunkProviderClient implements CubeProvider
{
	private World m_world;
	private BlankColumn m_blankColumn;
	
	public CubeProviderClient( World world )
	{
		super( world );
		
		m_world = world;
		m_blankColumn = new BlankColumn( world, 0, 0 );
	}
	
	@Override
	public Column loadChunk( int cubeX, int cubeZ )
	{
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping( this );
		Column column = (Column)chunkMapping.getValueByKey( ChunkCoordIntPair.chunkXZ2Int( cubeX, cubeZ ) );
		if( column != null )
		{
			return column;
		}
		
		// make a new one
		column = new Column( m_world, cubeX, cubeZ );
		
		chunkMapping.add( ChunkCoordIntPair.chunkXZ2Int( cubeX, cubeZ ), column );
		ChunkProviderClientAccessor.getChunkListing( this ).add( column );
		
		column.isChunkLoaded = true;
		return column;
	}
	
	@Override
	public Column provideChunk( int cubeX, int cubeZ )
	{
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping( this );
		Column column = (Column)chunkMapping.getValueByKey( ChunkCoordIntPair.chunkXZ2Int( cubeX, cubeZ ) );
		if( column != null )
		{
			return column;
		}
		
		return m_blankColumn;
	}
	
	@Override
	public boolean cubeExists( int cubeX, int cubeY, int cubeZ )
	{
		// cubes always exist on the client
		return true;
	}
	
	@Override
	public Cube provideCube( int cubeX, int cubeY, int cubeZ )
	{
		Cube cube = loadChunk( cubeX, cubeZ ).getOrCreateCube( cubeY, false );
		
		// cubes are always live on the client
		cube.setGeneratorStage( GeneratorStage.getLastStage() );
		
		return cube;
	}
}
