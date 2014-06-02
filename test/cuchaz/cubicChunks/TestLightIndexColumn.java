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

import cuchaz.cubicChunks.world.LightIndexColumn;

public class TestLightIndexColumn
{
	private static final int SeaLevel = 0;
	
	@Test
	public void readZero( )
	{
		LightIndexColumn index = new LightIndexColumn( SeaLevel );
		for( int i=-16; i<=16; i++ )
		{
			assertEquals( 0, index.getOpacity( i ) );
		}
	}
	
	@Test
	public void writeBottomDiffAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+0, 0 );
			
			index.setOpacity( i+0, 1 );
			
			assertEquals( 1, index.getOpacity( i ) );
			assertEquals( 0, index.getOpacity( i+1 ) );
		}
	}
	
	@Test
	public void writeBottomSameAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+0, 0, i+1, 1 );
			
			index.setOpacity( i+0, 1 );
			
			assertEquals( 1, index.getOpacity( i+0 ) );
			assertEquals( 1, index.getOpacity( i+1 ) );
		}
	}
	
	@Test
	public void writeMiddleSameBottomSameAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+0, 1, i+5, 0, i+6, 1 );
			
			index.setOpacity( i+5, 1 );
			
			assertEquals( 1, index.getOpacity( i+4 ) );
			assertEquals( 1, index.getOpacity( i+5 ) );
			assertEquals( 1, index.getOpacity( i+6 ) );
		}
	}
	
	@Test
	public void writeMiddleDiffBottomSameAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+6, 1 );
			
			index.setOpacity( i+5, 1 );
			
			assertEquals( 0, index.getOpacity( i+4 ) );
			assertEquals( 1, index.getOpacity( i+5 ) );
			assertEquals( 1, index.getOpacity( i+6 ) );
		}
	}
	
	@Test
	public void writeMiddleDiffBottomDiffAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+0, 0 );
			
			index.setOpacity( i+5, 1 );
			
			assertEquals( 0, index.getOpacity( i+4 ) );
			assertEquals( 1, index.getOpacity( i+5 ) );
			assertEquals( 0, index.getOpacity( i+6 ) );
		}
	}
	
	@Test
	public void writeMiddleSameBottomDiffAbove( )
	{
		for( int i=-16; i<=16; i++ )
		{
			LightIndexColumn index = buildColumn( i+0, 1, i+6, 0 );
			
			index.setOpacity( i+5, 1 );
			
			assertEquals( 1, index.getOpacity( i+4 ) );
			assertEquals( 1, index.getOpacity( i+5 ) );
			assertEquals( 0, index.getOpacity( i+6 ) );
		}
	}
	
	@Test
	public void topNonTransparentBlock( )
	{
		LightIndexColumn index = new LightIndexColumn( SeaLevel );
		
		assertEquals( null, index.getTopNonTransparentBlockY() );
		
		index.setOpacity( -16, 1 );
		assertEquals( -16, (int)index.getTopNonTransparentBlockY() );
		
		index.setOpacity( 0, 1 );
		assertEquals( 0, (int)index.getTopNonTransparentBlockY() );

		index.setOpacity( 1, 1 );
		assertEquals( 1, (int)index.getTopNonTransparentBlockY() );
		
		index.setOpacity( 5, 1 );
		assertEquals( 5, (int)index.getTopNonTransparentBlockY() );
		
		index.setOpacity( 5, 0 );
		assertEquals( 1, (int)index.getTopNonTransparentBlockY() );

		index.setOpacity( 1, 0 );
		assertEquals( 0, (int)index.getTopNonTransparentBlockY() );
		
		index.setOpacity( 0, 0 );
		assertEquals( -16, (int)index.getTopNonTransparentBlockY() );
		
		index.setOpacity( -16, 0 );
		assertEquals( null, index.getTopNonTransparentBlockY() );
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
				out.writeInt( data[i++] );
				out.writeByte( data[i++] );
			}
			out.close();
			
			// read the data
			LightIndexColumn index = new LightIndexColumn( SeaLevel );
			index.readData( new DataInputStream( new ByteArrayInputStream( buf.toByteArray() ) ) );
			return index;
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
}
