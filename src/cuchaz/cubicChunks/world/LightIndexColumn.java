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
	private TreeMap<Integer,Integer> m_opacity;
	private int m_topNonTransparentBlockY;
	
	public LightIndexColumn( )
	{
		// UNDONE: could reduce memory usage by using the int-packed representation in memory
		// although, that's probably not necessary since using cubic chunks should actually save memory
		// will have to do memory profiling to see if it's even worth trying to optimize memory usage here
		m_opacity = new TreeMap<Integer,Integer>();
		
		// init the column with all 0 opacity
		m_opacity.put( 0, 0 );
		m_topNonTransparentBlockY = -1;
	}
	
	public void readData( DataInputStream in )
	throws IOException
	{
		m_opacity.clear();
		int numEntries = in.readUnsignedShort();
		for( int i=0; i<numEntries; i++ )
		{
			int blockY = in.readUnsignedShort();
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
			out.writeShort( entry.getKey() );
			out.writeByte( entry.getValue() );
		}
	}
	
	public int getOpacity( int blockY )
	{
		// lookups are easy: log(n) time
		
		Map.Entry<Integer,Integer> entry = m_opacity.floorEntry( blockY );
		assert( entry != null );
		return entry.getValue();
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
		
		// is this the bottom block?
		if( blockY == 0 )
		{
			int opacityAbove = getOpacity( blockY + 1 );
			boolean sameAbove = opacity == opacityAbove;
			if( sameAbove )
			{
				// move the above entry down
				m_opacity.remove( blockY + 1 );
				m_opacity.put( blockY, opacity );
			}
			else // !sameAbove
			{
				// add/replace entries
				m_opacity.put( blockY, opacity );
				m_opacity.put( blockY + 1, opacityAbove );
			}
			
			// just in case
			assert( getOpacity( blockY ) == opacity );
			assert( getOpacity( blockY + 1 ) == opacityAbove );
		}
		else
		{
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
		}
		
		// did the top opacity change?
		if( opacity > 0 )
		{
			if( blockY > m_topNonTransparentBlockY )
			{
				// we added a new top non-transparent block, update cache
				m_topNonTransparentBlockY = blockY;
			}
		}
		else
		{
			if( blockY == m_topNonTransparentBlockY )
			{
				// we removed the top non-transparent block, invalidate cache
				m_topNonTransparentBlockY = -1;
			}
		}
	}
	
	public int getTopNonTransparentBlockY( )
	{
		// do we need to recompute this?
		if( m_topNonTransparentBlockY == -1 )
		{
			m_topNonTransparentBlockY = 0;
			int lastBlockY = 0;
			for( Map.Entry<Integer,Integer> entry : m_opacity.descendingMap().entrySet() )
			{
				int opacity = entry.getValue();
				if( opacity > 0 )
				{
					// if there was no last segment, that means the top entry in this column is non-transparent
					// which means the top non-transparent block is at infinity
					// obviously that doesn't make any sense
					assert( lastBlockY != 0 );
					
					// go to the top of this segment
					m_topNonTransparentBlockY = lastBlockY - 1;
					break;
				}
				
				lastBlockY = entry.getKey();
			}
		}
		
		return m_topNonTransparentBlockY;
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
