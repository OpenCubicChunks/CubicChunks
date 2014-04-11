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
package cuchaz.cubicChunks;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class LightIndexColumn
{
	private TreeMap<Integer,Integer> m_opacity;
	
	public LightIndexColumn( )
	{
		m_opacity = new TreeMap<Integer,Integer>();
		
		// init the column with all 0 opacity
		m_opacity.put( 0, 0 );
	}
	
	public LightIndexColumn( int[] data )
	{
		m_opacity = new TreeMap<Integer,Integer>();
		for( int datum : data )
		{
			// the first entry always gets added
			int blockY1 = unpackBlockY1( datum );
			int opacity1 = unpackOpacity1( datum );
			m_opacity.put( blockY1, opacity1 );
			
			// the second entry can't have a zero y value, so if this value is zero, then there's no entry here
			int blockY2 = unpackBlockY2( datum );
			int opacity2 = unpackOpacity2( datum );
			if( blockY2 > 0 )
			{
				m_opacity.put( blockY2, opacity2 );
			}
		}
	}
	
	public int[] getData( )
	{
		int[] data = new int[m_opacity.size()];
		int i = 0;
		Iterator<Map.Entry<Integer,Integer>> iter = m_opacity.entrySet().iterator();
		while( iter.hasNext() )
		{
			Map.Entry<Integer,Integer> entry1 = iter.next();
			if( iter.hasNext() )
			{
				// we have two entries
				Map.Entry<Integer,Integer> entry2 = iter.next();
				
				// pack both into the same integer
				data[i++] = pack( entry1.getKey(), entry1.getValue(), entry2.getKey(), entry2.getValue() );
			}
			else
			{
				// we have just one entry
				// pack it with a big fat zero
				data[i++] = pack( entry1.getKey(), entry1.getValue(), 0, 0 );
			}
		}
		return data;
	}
	
	public static int pack( int blockY1, int opacity1, int blockY2, int opacity2 )
	{
		// light opacity is [0,255], all blocks 0, 255 except ice,water:3, web:1
		// we can just move anything >15 down to 15 and then opacity fits into 4 bytes
		if( opacity1 > 15 )
		{
			opacity1 = 15;
		}
		if( opacity2 > 15 )
		{
			opacity2 = 15;
		}
		
		return Bits.packUnsignedToInt( blockY1, 12, 0 )
			| Bits.packUnsignedToInt( opacity1, 4, 12 )
			| Bits.packUnsignedToInt( blockY2, 12, 16 )
			| Bits.packUnsignedToInt( opacity2, 4, 28 );
	}
	
	public static int unpackBlockY1( int packed )
	{
		return Bits.unpackUnsigned( packed, 12, 0 );
	}
	
	public static int unpackOpacity1( int packed )
	{
		return Bits.unpackUnsigned( packed, 4, 12 );
	}
	
	public static int unpackBlockY2( int packed )
	{
		return Bits.unpackUnsigned( packed, 12, 16 );
	}
	
	public static int unpackOpacity2( int packed )
	{
		return Bits.unpackUnsigned( packed, 4, 28 );
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
	}
}
