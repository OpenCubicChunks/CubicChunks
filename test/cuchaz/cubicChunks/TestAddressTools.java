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

import java.util.HashSet;

import org.junit.Test;


public class TestAddressTools
{
	@Test
	public void testY( )
	{
		assertEquals( 1048575, AddressTools.MaxY );
		for( int i=0; i<=AddressTools.MaxY; i++ )
		{
			assertEquals( i, AddressTools.getY( AddressTools.getAddress( 0, i, 0 ) ) );
		}
	}
	
	@Test
	public void testX( )
	{
		assertEquals( -2097152, AddressTools.MinX );
		assertEquals( 2097151, AddressTools.MaxX );
		for( int i=AddressTools.MinX; i<=AddressTools.MaxX; i++ )
		{
			assertEquals( i, AddressTools.getX( AddressTools.getAddress( i, 0, 0 ) ) );
		}
	}
	
	@Test
	public void testZ( )
	{
		assertEquals( -2097152, AddressTools.MinZ );
		assertEquals( 2097151, AddressTools.MaxZ );
		for( int i=AddressTools.MinZ; i<=AddressTools.MaxZ; i++ )
		{
			assertEquals( i, AddressTools.getZ( AddressTools.getAddress( 0, 0, i ) ) );
		}
	}
	
	@Test
	public void testAddresses( )
	{
		for( int x=-32; x<=32; x++ )
		{
			for( int y=0; y<=64; y++ )
			{
				for( int z=-32; z<=32; z++ )
				{
					long address = AddressTools.getAddress( x, y, z );
					assertEquals( x, AddressTools.getX( address ) );
					assertEquals( y, AddressTools.getY( address ) );
					assertEquals( z, AddressTools.getZ( address ) );
				}
			}
		}
	}
	
	@Test
	public void testCollisions( )
	{
		HashSet<Long> addresses = new HashSet<Long>();
		for( int x=-32; x<=32; x++ )
		{
			for( int y=0; y<=64; y++ )
			{
				for( int z=-32; z<=32; z++ )
				{
					long address = AddressTools.getAddress( x, y, z );
					assertFalse( addresses.contains( address ) );
					addresses.add( address );
				}
			}
		}
	}
}
