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

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public class WorldAccessor
{
	private static Field m_fieldChunkProvider;
	
	static
	{
		try
		{
			m_fieldChunkProvider = World.class.getDeclaredField( "chunkProvider" );
			m_fieldChunkProvider.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static IChunkProvider getChunkProvider( World world )
	{
		try
		{
			return (IChunkProvider)m_fieldChunkProvider.get( world );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
}
