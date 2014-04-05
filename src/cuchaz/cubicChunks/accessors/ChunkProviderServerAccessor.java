/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.accessors;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class ChunkProviderServerAccessor
{
	private static Field m_fieldLoadedChunks;
	
	static
	{
		try
		{
			m_fieldLoadedChunks = ChunkProviderServer.class.getDeclaredField( "loadedChunks" );
			m_fieldLoadedChunks.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static List<Chunk> getLoadedChunks( ChunkProviderServer provider )
	{
		try
		{
			return (List<Chunk>)m_fieldLoadedChunks.get( provider );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
}
