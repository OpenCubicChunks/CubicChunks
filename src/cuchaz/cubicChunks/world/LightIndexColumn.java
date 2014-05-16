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
package cuchaz.cubicChunks.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class LightIndexColumn
{
	// UNDONE: do something smarter about sea level
	private static final int SeaLevel = 0;
	
	private TreeMap<Integer,Integer> m_opacity;
	private int m_topNonTransparentBlockY;
	private int m_topOpaqueUnderSeaLevelBlockY;
	
	public LightIndexColumn( )
	{
		// UNDONE: could reduce memory usage by using the int-packed representation in memory
		// although, that's probably not necessary since using cubes should actually save memory
		// will have to do memory profiling to see if it's even worth trying to optimize memory usage here
		m_opacity = new TreeMap<Integer,Integer>();
		
		m_topNonTransparentBlockY = Integer.MIN_VALUE;
		m_topOpaqueUnderSeaLevelBlockY = Integer.MIN_VALUE;
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
		
		// update caches
		if( opacity > 0 && blockY > m_topNonTransparentBlockY )
		{
			// we added a new top non-transparent block, update cache
			m_topNonTransparentBlockY = blockY;
		}
		else if( opacity == 0 && blockY == m_topNonTransparentBlockY )
		{
			// we removed the top non-transparent block, invalidate cache
			m_topNonTransparentBlockY = Integer.MIN_VALUE;
		}
		
		if( blockY < SeaLevel )
		{
			if( opacity == 255 && blockY > m_topOpaqueUnderSeaLevelBlockY )
			{
				// we added a new opaque under sea level block
				m_topOpaqueUnderSeaLevelBlockY = blockY;
			}
			else if( opacity < 255 && blockY == m_topOpaqueUnderSeaLevelBlockY )
			{
				// we removed the top opaque under sea level block, invalidate cache
				m_topOpaqueUnderSeaLevelBlockY = Integer.MIN_VALUE;
			}
		}
	}
	
	public int getTopNonTransparentBlockY( )
	{
		// do we need to recompute this?
		if( m_topNonTransparentBlockY == Integer.MIN_VALUE )
		{
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
					m_topNonTransparentBlockY = lastEntry.getKey() - 1;
					break;
				}
				
				lastEntry = entry;
			}
		}
		return m_topNonTransparentBlockY;
	}
	
	public int getTopOpaqueBlockBelowSeaLevel( )
	{
		// do we need to recompute this?
		if( m_topOpaqueUnderSeaLevelBlockY == Integer.MIN_VALUE )
		{
			// UNDONE: this is a linear-time query... we might be able to speed it up if we need
			// log(n) would be nice
			
			Map.Entry<Integer,Integer> entry = m_opacity.floorEntry( SeaLevel );
			while( entry != null )
			{
				// is this segment opaque?
				if( entry.getValue() == 255 )
				{
					// go to the top of this segment
					// NOTE: since this segment is opaque, there should always be a transparent one above it
					m_topOpaqueUnderSeaLevelBlockY = m_opacity.higherKey( entry.getKey() );
					break;
				}
				
				// move down to the next entry
				entry = m_opacity.lowerEntry( entry.getKey() );
			}
		}
		return m_topOpaqueUnderSeaLevelBlockY;
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
