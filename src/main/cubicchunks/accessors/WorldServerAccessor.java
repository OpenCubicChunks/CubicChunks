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
package cubicchunks.accessors;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class WorldServerAccessor
{
	private static Field m_fieldScheduledTicksTreeSet;
	private static Field m_fieldScheduledTicksHashSet;
	private static Field m_fieldScheduledTicksNextTick;
	private static Field m_fieldChunkProvider;
	private static Field m_fieldPlayerManager;
	
	static
	{
		try
		{
			m_fieldScheduledTicksTreeSet = WorldServer.class.getDeclaredField( "pendingTickListEntriesTreeSet" );
			m_fieldScheduledTicksTreeSet.setAccessible( true );
			
			m_fieldScheduledTicksHashSet = WorldServer.class.getDeclaredField( "pendingTickListEntriesHashSet" );
			m_fieldScheduledTicksHashSet.setAccessible( true );
			
			m_fieldScheduledTicksNextTick = WorldServer.class.getDeclaredField( "pendingTickListEntriesThisTick" );
			m_fieldScheduledTicksNextTick.setAccessible( true );
			
			m_fieldChunkProvider = WorldServer.class.getDeclaredField( "theChunkProviderServer" );
			m_fieldChunkProvider.setAccessible( true );
			
			m_fieldPlayerManager = WorldServer.class.getDeclaredField( "thePlayerManager" );
			m_fieldPlayerManager.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static TreeSet<NextTickListEntry> getScheduledTicksTreeSet( WorldServer worldServer )
	{
		try
		{
			return (TreeSet<NextTickListEntry>)m_fieldScheduledTicksTreeSet.get( worldServer );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static Set<NextTickListEntry> getScheduledTicksHashSet( WorldServer worldServer )
	{
		try
		{
			return (Set<NextTickListEntry>)m_fieldScheduledTicksHashSet.get( worldServer );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static List<NextTickListEntry> getScheduledTicksThisTick( WorldServer worldServer )
	{
		try
		{
			return (List<NextTickListEntry>)m_fieldScheduledTicksNextTick.get( worldServer );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static ChunkProviderServer getChunkProvider( WorldClient world )
	{
		try
		{
			return (ChunkProviderServer)m_fieldChunkProvider.get( world );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static void setChunkProvider( WorldServer world, ChunkProviderServer val )
	{
		try
		{
			m_fieldChunkProvider.set( world, val );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static PlayerManager getPlayerManager( WorldClient world )
	{
		try
		{
			return (PlayerManager)m_fieldPlayerManager.get( world );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static void setPlayerManager( WorldServer world, PlayerManager val )
	{
		try
		{
			m_fieldPlayerManager.set( world, val );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
}
