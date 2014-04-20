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
package cuchaz.cubicChunks.accessors;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;


public class ChunkProviderClientAccessor
{
	private static Field m_fieldChunkMapping;
	private static Field m_fieldChunkListing;
	private static Field m_fieldBlankChunk;
	
	static
	{
		try
		{
			m_fieldChunkMapping = ChunkProviderClient.class.getDeclaredField( "chunkMapping" );
			m_fieldChunkMapping.setAccessible( true );
			
			m_fieldChunkListing = ChunkProviderClient.class.getDeclaredField( "chunkListing" );
			m_fieldChunkListing.setAccessible( true );
			
			m_fieldBlankChunk = ChunkProviderClient.class.getDeclaredField( "blankChunk" );
			m_fieldBlankChunk.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static LongHashMap getChunkMapping( ChunkProviderClient provider )
	{
		try
		{
			return (LongHashMap)m_fieldChunkMapping.get( provider );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static List<Chunk> getChunkListing( ChunkProviderClient provider )
	{
		try
		{
			return (List<Chunk>)m_fieldChunkListing.get( provider );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static Chunk getBlankChunk( ChunkProviderClient provider )
	{
		try
		{
			return (Chunk)m_fieldBlankChunk.get( provider );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static void setBlankChunk( ChunkProviderClient provider, Chunk val )
	{
		try
		{
			m_fieldBlankChunk.set( provider, val );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
}
