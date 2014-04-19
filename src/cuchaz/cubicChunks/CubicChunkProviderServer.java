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

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class CubicChunkProviderServer extends ChunkProviderServer
{
	public CubicChunkProviderServer( WorldServer world )
	{
		super(
			world,
			new CubicChunkLoader( world.getSaveHandler() ),
			new CubicChunkGenerator( world )
		);
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
	
	public boolean cubicChunkExists( int chunkX, int chunkY, int chunkZ )
	{
		Column column = loadChunk( chunkX, chunkZ );
		return column.getCubicChunk( chunkY ) != null;
	}
	
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
