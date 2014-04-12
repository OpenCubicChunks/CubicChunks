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

import java.lang.reflect.Method;

import net.minecraft.world.chunk.Chunk;

public class ChunkAccessor
{
	private static Method m_methodPropagateSkylightOcclusion;
	
	static
	{
		try
		{
			m_methodPropagateSkylightOcclusion = Chunk.class.getDeclaredMethod( "propagateSkylightOcclusion", int.class, int.class );
			m_methodPropagateSkylightOcclusion.setAccessible( true );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
	
	public static void propagateSkylightOcclusion( Chunk chunk, int localX, int localZ )
	{
		try
		{
			m_methodPropagateSkylightOcclusion.invoke( chunk, localX, localZ );
		}
		catch( Exception ex )
		{
			throw new Error( ex );
		}
	}
}
