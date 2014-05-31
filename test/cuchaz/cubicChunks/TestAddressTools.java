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

import cuchaz.cubicChunks.util.CubeAddress;


public class TestAddressTools
{
	@Test
	public void testY( )
	{
		assertEquals( -524288, CubeAddress.MinY );
		assertEquals( 524287, CubeAddress.MaxY );
		for( int i=CubeAddress.MinY; i<=CubeAddress.MaxY; i++ )
		{
			assertEquals( i, CubeAddress.getY( CubeAddress.getAddress( 0, i, 0 ) ) );
		}
	}
	
	@Test
	public void testX( )
	{
		assertEquals( -2097152, CubeAddress.MinX );
		assertEquals( 2097151, CubeAddress.MaxX );
		for( int i=CubeAddress.MinX; i<=CubeAddress.MaxX; i++ )
		{
			assertEquals( i, CubeAddress.getX( CubeAddress.getAddress( i, 0, 0 ) ) );
		}
	}
	
	@Test
	public void testZ( )
	{
		assertEquals( -2097152, CubeAddress.MinZ );
		assertEquals( 2097151, CubeAddress.MaxZ );
		for( int i=CubeAddress.MinZ; i<=CubeAddress.MaxZ; i++ )
		{
			assertEquals( i, CubeAddress.getZ( CubeAddress.getAddress( 0, 0, i ) ) );
		}
	}
	
	@Test
	public void testAddresses( )
	{
		for( int x=-32; x<=32; x++ )
		{
			for( int y=-32; y<=32; y++ )
			{
				for( int z=-32; z<=32; z++ )
				{
					long address = CubeAddress.getAddress( x, y, z );
					assertEquals( x, CubeAddress.getX( address ) );
					assertEquals( y, CubeAddress.getY( address ) );
					assertEquals( z, CubeAddress.getZ( address ) );
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
			for( int y=-32; y<=32; y++ )
			{
				for( int z=-32; z<=32; z++ )
				{
					long address = CubeAddress.getAddress( x, y, z );
					assertFalse( addresses.contains( address ) );
					addresses.add( address );
				}
			}
		}
	}
}
