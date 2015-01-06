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
package main.java.cubicchunks.accessors;

import java.lang.reflect.Field;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;

public class WorldClientAccessor
{
	private static Field m_fieldChunkProvider;
	
	static
	{
		try
		{
			m_fieldChunkProvider = WorldClient.class.getDeclaredField( "clientChunkProvider" );
			m_fieldChunkProvider.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static ChunkProviderClient getChunkProvider( WorldClient world )
	{
		try
		{
			return (ChunkProviderClient)m_fieldChunkProvider.get( world );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static void setChunkProvider( WorldClient world, ChunkProviderClient val )
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
}
