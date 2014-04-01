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


public class Address implements Comparable<Address>
{
	public int dimension;
	public int x;
	public int y;
	public int z;
	
	public Address( )
	{
		dimension = 0;
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Address( int dimension, int x, int y, int z )
	{
		set( dimension, x, y, z );
	}
	
	public Address( Address other )
	{
		set( other.dimension, other.x, other.y, other.z );
	}
	
	public void set( int dimension, int x, int y, int z )
	{
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public int hashCode( )
	{
		return x + ( y << 8 ) + ( z << 16 ) + ( dimension << 30 );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof Address )
		{
			return equals( (Address)other );
		}
		return false;
	}
	
	public boolean equals( Address other )
	{
		return dimension == other.dimension && x == other.x && y == other.y && z == other.z;
	}
	
	@Override
    public String toString( )
    {
    	return String.format( "(%d,%d,%d,%d)", dimension, x, y, z );
    }

	@Override
	public int compareTo( Address other )
	{
		int diff = dimension - other.dimension;
		if( diff != 0 )
		{
			return diff;
		}
		diff = x - other.x;
		if( diff != 0 )
		{
			return diff;
		}
		diff = y - other.y;
		if( diff != 0 )
		{
			return diff;
		}
		return z - other.z;
	}
}
