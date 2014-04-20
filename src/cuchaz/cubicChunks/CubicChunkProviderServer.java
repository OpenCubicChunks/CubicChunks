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

import cuchaz.cubicChunks.accessors.ChunkProviderServerAccessor;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class CubicChunkProviderServer extends ChunkProviderServer implements CubicChunkProvider
{
	public CubicChunkProviderServer( WorldServer world )
	{
		super(
			world,
			new CubicChunkLoader( world.getSaveHandler() ),
			new CubicChunkGenerator( world )
		);
		
		// set empty chunk
		ChunkProviderServerAccessor.setBlankChunk( this, new BlankColumn( world, 0, 0 ) );
	}
	
	@Override
	public Column loadChunk( int chunkX, int chunkZ )
	{
		return (Column)super.loadChunk( chunkX, chunkZ );
	}
	
	@Override
	public Column provideChunk( int chunkX, int chunkZ )
	{
		return (Column)super.provideChunk( chunkX, chunkZ );
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
	public CubicChunk loadCubicChunk( int chunkX, int chunkY, int chunkZ )
	{
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
	
	public void unloadCubicChunkIfNotNearSpawn( CubicChunk cubicChunk )
	{
		unloadCubicChunkIfNotNearSpawn( cubicChunk.getX(), cubicChunk.getY(), cubicChunk.getZ() );
	}
	
	public void unloadCubicChunkIfNotNearSpawn( int chunkX, int chunkY, int chunkZ )
	{
		// UNDONE: implement meeee!!!!
		// implement an unload queue like the superclass does
	}
}
