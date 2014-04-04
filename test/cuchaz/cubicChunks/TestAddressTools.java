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
		assertEquals( -128, AddressTools.MinDimension );
		assertEquals( 127, AddressTools.MaxDimension );
		for( int i=AddressTools.MinDimension; i<=AddressTools.MaxDimension; i++ )
		{
			assertEquals( i, AddressTools.getDimension( AddressTools.getAddress( i, 0, 0, 0 ) ) );
		}
	}
	
	@Test
	public void testY( )
	{
		assertEquals( -2048, AddressTools.MinY );
		assertEquals( 2047, AddressTools.MaxY );
		for( int i=AddressTools.MinY; i<=AddressTools.MaxY; i++ )
		{
			assertEquals( i, AddressTools.getY( AddressTools.getAddress( 0, 0, i, 0 ) ) );
		}
	}
	
	@Test
	public void testX( )
	{
		assertEquals( -2097152, AddressTools.MinX );
		assertEquals( 2097151, AddressTools.MaxX );
		for( int i=AddressTools.MinX; i<=AddressTools.MaxX; i++ )
		{
			assertEquals( i, AddressTools.getX( AddressTools.getAddress( 0, i, 0, 0 ) ) );
		}
	}
	
	@Test
	public void testZ( )
	{
		assertEquals( -2097152, AddressTools.MinZ );
		assertEquals( 2097151, AddressTools.MaxZ );
		for( int i=AddressTools.MinZ; i<=AddressTools.MaxZ; i++ )
		{
			assertEquals( i, AddressTools.getZ( AddressTools.getAddress( 0, 0, 0, i ) ) );
		}
	}
	
	@Test
	public void testAddresses( )
	{
		for( int dimension=-2; dimension<=2; dimension++ )
		{
			for( int x=-16; x<=16; x++ )
			{
				for( int y=-16; y<=16; y++ )
				{
					for( int z=-16; z<=16; z++ )
					{
						long address = AddressTools.getAddress( dimension, x, y, z );
						assertEquals( dimension, AddressTools.getDimension( address ) );
						assertEquals( x, AddressTools.getX( address ) );
						assertEquals( y, AddressTools.getY( address ) );
						assertEquals( z, AddressTools.getZ( address ) );
					}
				}
			}
		}
	}
}
