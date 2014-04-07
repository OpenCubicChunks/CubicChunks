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
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import cuchaz.cubicChunks.accessors.ChunkProviderClientAccessor;

public class CubicChunkProviderClient extends ChunkProviderClient
{
	private World m_world;
	
	public CubicChunkProviderClient( World world )
	{
		super( world );
		
		m_world = world;
	}
	
	@Override
	public Column loadChunk( int chunkX, int chunkZ )
    {
		Column column = new Column( m_world, chunkX, chunkZ );
		
		ChunkProviderClientAccessor.getChunkMapping( this ).add( ChunkCoordIntPair.chunkXZ2Int( chunkX, chunkZ ), column );
		ChunkProviderClientAccessor.getChunkListing( this ).add( column );
        
        column.isChunkLoaded = true;
        return column;
    }
}
