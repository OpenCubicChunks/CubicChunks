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

import net.minecraft.util.ChunkCoordinates;

public class Coords implements Comparable<Coords>
{
	public int x;
	public int y;
	public int z;
	
	public Coords( )
	{
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Coords( int x, int y, int z )
	{
		set( x, y, z );
	}
	
	public Coords( Coords other )
	{
		set( other.x, other.y, other.z );
	}
	
	public Coords( ChunkCoordinates other )
	{
		set( other.posX, other.posY, other.posZ );
	}
	
	public void set( int x, int y, int z )
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public int hashCode( )
	{
		return x + ( y << 8 ) + ( z << 16 );
	}
	
	@Override
	public boolean equals( Object other )
	{
		if( other instanceof Coords )
		{
			return equals( (Coords)other );
		}
		return false;
	}
	
	public boolean equals( Coords other )
	{
		return x == other.x && y == other.y && z == other.z;
	}
	
	@Override
    public String toString( )
    {
    	return String.format( "(%d,%d,%d)", x, y, z );
    }

	@Override
	public int compareTo( Coords other )
	{
		int diff = x - other.x;
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
