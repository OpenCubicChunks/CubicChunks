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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LightIndex
{
	private LightIndexColumn[] m_columns;
	private int m_topNonTransparentBlockY;
	
	public LightIndex( )
	{
		m_columns = new LightIndexColumn[16*16];
		for( int i=0; i<m_columns.length; i++ )
		{
			m_columns[i] = new LightIndexColumn();
		}
		
		m_topNonTransparentBlockY = -1;
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
		
		// invalidate top block cache
		m_topNonTransparentBlockY = -1;
	}
	
	public int getTopNonTransparentBlock( int localX, int localZ )
	{
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].getTopNonTransparentBlockY();
	}
	
	public int getTopNonTransparentBlockY( )
	{
		// do we need to update the cache?
		if( m_topNonTransparentBlockY == -1 )
		{
			m_topNonTransparentBlockY = 0;
			for( int i=0; i<m_columns.length; i++ )
			{
				m_topNonTransparentBlockY = Math.max( m_topNonTransparentBlockY, m_columns[i].getTopNonTransparentBlockY() );
			}
		}
		return m_topNonTransparentBlockY;
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
