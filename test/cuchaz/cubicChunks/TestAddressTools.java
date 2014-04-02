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

import static org.junit.Assert.*;

import org.junit.Test;


public class TestAddressTools
{
	@Test
	public void testDimension( )
	{
		assertEquals( 0, AddressTools.MinDimension );
		assertEquals( 15, AddressTools.MaxDimension );
		for( int i=AddressTools.MinDimension; i<=AddressTools.MaxDimension; i++ )
		{
			assertEquals( i, AddressTools.getDimension( AddressTools.toAddress( i, 0, 0, 0 ) ) );
		}
		
		// and test dimension -1 just because it's actually used in vanilla Minecraft
		assertEquals( 15, AddressTools.getDimension( AddressTools.toAddress( -1, 0, 0, 0 ) ) );
	}
	
	@Test
	public void testY( )
	{
		assertEquals( -2048, AddressTools.MinY );
		assertEquals( 2047, AddressTools.MaxY );
		for( int i=AddressTools.MinY; i<=AddressTools.MaxY; i++ )
		{
			assertEquals( i, AddressTools.getY( AddressTools.toAddress( 0, 0, i, 0 ) ) );
		}
	}
	
	@Test
	public void testX( )
	{
		assertEquals( -32768, AddressTools.MinX );
		assertEquals( 32767, AddressTools.MaxX );
		for( int i=AddressTools.MinX; i<=AddressTools.MaxX; i++ )
		{
			assertEquals( i, AddressTools.getX( AddressTools.toAddress( 0, i, 0, 0 ) ) );
		}
	}
	
	@Test
	public void testZ( )
	{
		assertEquals( -32768, AddressTools.MinZ );
		assertEquals( 32767, AddressTools.MaxZ );
		for( int i=AddressTools.MinZ; i<=AddressTools.MaxZ; i++ )
		{
			assertEquals( i, AddressTools.getZ( AddressTools.toAddress( 0, 0, 0, i ) ) );
		}
	}
}
