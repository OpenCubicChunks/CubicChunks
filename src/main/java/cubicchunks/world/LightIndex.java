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
package main.java.cubicchunks.world;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import main.java.cubicchunks.util.ValueCache;

public class LightIndex
{
	private LightIndexColumn[] m_columns;
	private ValueCache<Integer> m_topNonTransparentBlockY;
	
	public LightIndex( int seaLevel )
	{
		m_columns = new LightIndexColumn[16*16];
		for( int i=0; i<m_columns.length; i++ )
		{
			m_columns[i] = new LightIndexColumn( seaLevel );
		}
		
		m_topNonTransparentBlockY = new ValueCache<Integer>();
	}
	
	public int getOpacity( int localX, int blockY, int localZ )
	{
		int xzCoord = localZ << 4 | localX;
		if( m_columns[xzCoord] == null )
		{
			return 0;
		}
		return m_columns[xzCoord].getOpacity( blockY );
	}
	
	public void setOpacity( int localX, int blockY, int localZ, int opacity )
	{
		int xzCoord = localZ << 4 | localX;
		m_columns[xzCoord].setOpacity( blockY, opacity );
		
		m_topNonTransparentBlockY.clear();
	}
	
	public Integer getTopNonTransparentBlockY( int localX, int localZ )
	{
		int xzCoord = localZ << 4 | localX;
        LightIndexColumn column = m_columns[xzCoord];
        assert column != null;
        return column.getTopNonTransparentBlockY();
	}
	
	public Integer getTopNonTransparentBlockY( )
	{
		// do we need to update the cache?
		if( !m_topNonTransparentBlockY.hasValue() )
		{
			m_topNonTransparentBlockY.set( null );
			
			for( int i=0; i<m_columns.length; i++ )
			{
				// get the top y from the column
				Integer blockY = m_columns[i].getTopNonTransparentBlockY();
				if( blockY == null )
				{
					continue;
				}
				
				// does it beat our current top y?
				if( m_topNonTransparentBlockY.get() == null || blockY > m_topNonTransparentBlockY.get() )
				{
					m_topNonTransparentBlockY.set( blockY );
				}
			}
		}
		return m_topNonTransparentBlockY.get();
	}
	
	public Integer getTopOpaqueBlockBelowSeaLevel( int localX, int localZ )
	{
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].getTopOpaqueBlockBelowSeaLevel();
	}
	
	public byte[] getData( )
	{
		try
		{
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream( buf );
			writeData( out );
			out.close();
			return buf.toByteArray();
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	public void readData( byte[] data )
	{
		try
		{
			ByteArrayInputStream buf = new ByteArrayInputStream( data );
			DataInputStream in = new DataInputStream( buf );
			readData( in );
			in.close();
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	public void readData( DataInputStream in )
	throws IOException
	{
		for( int i=0; i<m_columns.length; i++ )
		{
			m_columns[i].readData( in );
		}
	}
	
	public void writeData( DataOutputStream out )
	throws IOException
	{
		for( int i=0; i<m_columns.length; i++ )
		{
			m_columns[i].writeData( out );
		}
	}
	
	public String dump( int localX, int localZ )
	{
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].dump();
	}
}
