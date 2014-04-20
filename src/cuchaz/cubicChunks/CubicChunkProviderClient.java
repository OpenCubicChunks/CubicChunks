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

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import cuchaz.cubicChunks.accessors.ChunkProviderClientAccessor;

public class CubicChunkProviderClient extends ChunkProviderClient implements CubicChunkProvider
{
	private World m_world;
	
	public CubicChunkProviderClient( World world )
	{
		super( world );
		
		m_world = world;
		
		// set empty chunk
		ChunkProviderClientAccessor.setBlankChunk( this, new BlankColumn( world, 0, 0 ) );
	}
	
	@Override
	public Column loadChunk( int chunkX, int chunkZ )
	{
		// is this chunk already loaded?
		LongHashMap chunkMapping = ChunkProviderClientAccessor.getChunkMapping( this );
		Column column = (Column)chunkMapping.getValueByKey( ChunkCoordIntPair.chunkXZ2Int( chunkX, chunkZ ) );
		if( column != null )
		{
			return column;
		}
		
		// make a new one
		column = new Column( m_world, chunkX, chunkZ );
		
		chunkMapping.add( ChunkCoordIntPair.chunkXZ2Int( chunkX, chunkZ ), column );
		ChunkProviderClientAccessor.getChunkListing( this ).add( column );
		
		column.isChunkLoaded = true;
		return column;
	}
	
	@Override
	public boolean cubicChunkExists( int chunkX, int chunkY, int chunkZ )
	{
		// NOTE: cubic chunks always exist on the client
		// but unloaded cubic chunks will be empty
		return true;
	}
	
	@Override
	public CubicChunk loadCubicChunk( int chunkX, int chunkY, int chunkZ )
	{
		// UNDONE: implement this
		throw new UnsupportedOperationException();
	}
}
