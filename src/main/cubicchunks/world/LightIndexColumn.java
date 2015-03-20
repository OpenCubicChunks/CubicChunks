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
package cubicchunks.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import cubicchunks.util.ValueCache;

public class LightIndexColumn
{
	private int m_seaLevel;
	private TreeMap<Integer,Integer> m_opacity;
	private ValueCache<Integer> m_topNonTransparentBlockY;
	private ValueCache<Integer> m_topOpaqueUnderSeaLevelBlockY;
	
	public LightIndexColumn( int seaLevel )
	{
		m_seaLevel = seaLevel;
		// UNDONE: could reduce memory usage by using the int-packed representation in memory
		// although, that's probably not necessary since using cubes should actually save memory
		// will have to do memory profiling to see if it's even worth trying to optimize memory usage here
		m_opacity = new TreeMap<Integer,Integer>();
		
		m_topNonTransparentBlockY = new ValueCache<Integer>();
		m_topOpaqueUnderSeaLevelBlockY = new ValueCache<Integer>();
	}
	
	public void readData( DataInputStream in )
	throws IOException
	{
		m_opacity.clear();
		int numEntries = in.readUnsignedShort();
		for( int i=0; i<numEntries; i++ )
		{
			int blockY = in.readInt();
			int opacity = in.readUnsignedByte();
			m_opacity.put( blockY, opacity );
		}
	}
	
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeShort( m_opacity.size() );
		for( Map.Entry<Integer,Integer> entry : m_opacity.entrySet() )
		{
			out.writeInt( entry.getKey() );
			out.writeByte( entry.getValue() );
		}
	}
	
	public int getOpacity( int blockY )
	{
		// lookups are easy: log(n) time
		
		Map.Entry<Integer,Integer> entry = m_opacity.floorEntry( blockY );
		if( entry != null )
		{
			return entry.getValue();
		}
		
		// assume zero
		return 0;
	}
	
	public void setOpacity( int blockY, int opacity )
	{
		// updates are a bit harder: 5log(n) time
		
		// do we need to change anything?
		int opacityAt = getOpacity( blockY );
		if( opacity == opacityAt )
		{
			return;
		}
		
		// updates fall into one of 4 categories based on the opacity of the blocks above and below
		int opacityAbove = getOpacity( blockY + 1 );
		int opacityBelow = getOpacity( blockY - 1 );
		boolean sameAbove = opacity == opacityAbove;
		boolean sameBelow = opacity == opacityBelow;
		if( sameAbove && sameBelow )
		{
			// remove this entry and the one above it
			m_opacity.remove( blockY );
			m_opacity.remove( blockY + 1 );
		}
		else if( sameAbove && !sameBelow )
		{
			// move the above entry down
			m_opacity.remove( blockY + 1 );
			m_opacity.put( blockY, opacity );
		}
		else if( !sameAbove && sameBelow )
		{
			// move the entry up
			m_opacity.remove( blockY );
			m_opacity.put( blockY + 1, opacityAbove );
		}
		else // !sameAbove && !sameBelow
		{
			// add/replace entries
			m_opacity.put( blockY, opacity );
			m_opacity.put( blockY + 1, opacityAbove );
		}
		
		// just in case
		assert( getOpacity( blockY ) == opacity );
		assert( getOpacity( blockY + 1 ) == opacityAbove );
		assert( getOpacity( blockY - 1 ) == opacityBelow );
		
		// invalidate caches
		m_topNonTransparentBlockY.clear();
		m_topOpaqueUnderSeaLevelBlockY.clear();
	}
	
	public Integer getTopNonTransparentBlockY( )
	{
		// do we need to recompute this?
		if( !m_topNonTransparentBlockY.hasValue() )
		{
			m_topNonTransparentBlockY.set( null );
			
			Map.Entry<Integer,Integer> lastEntry = null;
			for( Map.Entry<Integer,Integer> entry : m_opacity.descendingMap().entrySet() )
			{
				int opacity = entry.getValue();
				if( opacity > 0 )
				{
					// if there was no last segment, that means the top entry in this column is non-transparent
					// which means the top non-transparent block is at infinity
					// obviously that doesn't make any sense
					assert( lastEntry != null );
					
					// go to the top of this segment
					m_topNonTransparentBlockY.set( lastEntry.getKey() - 1 );
					break;
				}
				
				lastEntry = entry;
			}
		}
		return m_topNonTransparentBlockY.get();
	}
	
	public Integer getTopOpaqueBlockBelowSeaLevel( )
	{
		// do we need to recompute this?
		if( !m_topOpaqueUnderSeaLevelBlockY.hasValue() )
		{
			m_topOpaqueUnderSeaLevelBlockY.set( null );
			
			// UNDONE: this is a linear-time query... we might be able to speed it up if we need
			// log(n) would be nice
			
			Map.Entry<Integer,Integer> entry = m_opacity.floorEntry( m_seaLevel );
			while( entry != null )
			{
				// is this segment opaque?
				if( entry.getValue() == 255 )
				{
					// go to the top of this segment
					// NOTE: since this segment is opaque, there should always be a transparent one above it
					m_topOpaqueUnderSeaLevelBlockY.set( m_opacity.higherKey( entry.getKey() ) );
					break;
				}
				
				// move down to the next entry
				entry = m_opacity.lowerEntry( entry.getKey() );
			}
		}
		return m_topOpaqueUnderSeaLevelBlockY.get();
	}

	public String dump( )
	{
		StringBuilder buf = new StringBuilder();
		for( Map.Entry<Integer,Integer> entry : m_opacity.entrySet() )
		{
			if( buf.length() > 0 )
			{
				buf.append( ", " );
			}
			buf.append( String.format( "%d:%d", entry.getKey(), entry.getValue() ) );
		}
		return buf.toString();
	}
}
