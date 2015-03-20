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
 ******************************************************************************/
package cubicchunks.accessors;

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
