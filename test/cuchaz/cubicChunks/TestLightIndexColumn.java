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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

public class TestLightIndexColumn
{
	@Test
	public void readZero( )
	{
		LightIndexColumn index = new LightIndexColumn();
		for( int i=0; i<16; i++ )
		{
			assertEquals( 0, index.getOpacity( i ) );
		}
	}
	
	@Test
	public void writeBottomDiffAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 0 );
		
		index.setOpacity( 0, 1 );
		
		assertEquals( 1, index.getOpacity( 0 ) );
		assertEquals( 0, index.getOpacity( 1 ) );
	}
	
	@Test
	public void writeBottomSameAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 0, 1, 1 );
		
		index.setOpacity( 0, 1 );
		
		assertEquals( 1, index.getOpacity( 0 ) );
		assertEquals( 1, index.getOpacity( 1 ) );
	}
	
	@Test
	public void writeMiddleSameBottomSameAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 1, 5, 0, 6, 1 );
		
		index.setOpacity( 5, 1 );
		
		assertEquals( 1, index.getOpacity( 4 ) );
		assertEquals( 1, index.getOpacity( 5 ) );
		assertEquals( 1, index.getOpacity( 6 ) );
	}
	
	@Test
	public void writeMiddleDiffBottomSameAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 0, 6, 1 );
		
		index.setOpacity( 5, 1 );
		
		assertEquals( 0, index.getOpacity( 4 ) );
		assertEquals( 1, index.getOpacity( 5 ) );
		assertEquals( 1, index.getOpacity( 6 ) );
	}
	
	@Test
	public void writeMiddleDiffBottomDiffAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 0 );
		
		index.setOpacity( 5, 1 );
		
		assertEquals( 0, index.getOpacity( 4 ) );
		assertEquals( 1, index.getOpacity( 5 ) );
		assertEquals( 0, index.getOpacity( 6 ) );
	}
	
	@Test
	public void writeMiddleSameBottomDiffAbove( )
	{
		LightIndexColumn index = buildColumn( 0, 1, 6, 0 );
		
		index.setOpacity( 5, 1 );
		
		assertEquals( 1, index.getOpacity( 4 ) );
		assertEquals( 1, index.getOpacity( 5 ) );
		assertEquals( 0, index.getOpacity( 6 ) );
	}
	
	private LightIndexColumn buildColumn( int ... data )
	{
		try
		{
			// write the data
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream( buf );
			out.writeShort( data.length/2 );
			for( int i=0; i<data.length; )
			{
				out.writeShort( data[i++] );
				out.writeByte( data[i++] );
			}
			out.close();
			
			// read the data
			LightIndexColumn index = new LightIndexColumn();
			index.readData( new DataInputStream( new ByteArrayInputStream( buf.toByteArray() ) ) );
			return index;
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
}
