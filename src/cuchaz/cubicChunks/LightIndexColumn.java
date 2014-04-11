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
			int blockY = unpackBlockY( datum );
			int opacity = unpackOpacity( datum );
			m_opacity.put( blockY, opacity );
		}
	}
	
	public int[] getData( )
	{
		int[] data = new int[m_opacity.size()];
		int i = 0;
		for( Map.Entry<Integer,Integer> entry : m_opacity.entrySet() )
		{
			data[i++] = pack( entry.getKey(), entry.getValue() );
		}
		return data;
	}
	
	public static int pack( int blockY, int opacity )
	{
		return Bits.packUnsignedToInt( blockY, 12, 8 )
			| Bits.packUnsignedToInt( opacity, 8, 0 );
	}
	
	public static int unpackBlockY( int packed )
	{
		return Bits.unpackUnsigned( packed, 12, 8 );
	}
	
	public static int unpackOpacity( int packed )
	{
		return Bits.unpackUnsigned( packed, 8, 0 );
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
