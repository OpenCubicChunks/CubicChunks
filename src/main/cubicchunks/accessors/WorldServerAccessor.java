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
