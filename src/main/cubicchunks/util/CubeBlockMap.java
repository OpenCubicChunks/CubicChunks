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
package cubicchunks.util;

import java.util.TreeMap;


public class CubeBlockMap<T> extends TreeMap<Integer,T>
{
	private static final long serialVersionUID = -356507892710221222L;
	
	// each coordinate is only 4 bits since a chunk is 16x16x16
	private static final int XSize = 4;
	private static final int YSize = 4;
	private static final int ZSize = 4;
	
	private static final int ZOffset = 0;
	private static final int YOffset = ZOffset + ZSize;
	private static final int XOffset = YOffset + YSize;
	
	public T put( int x, int y, int z, T val )
	{
		return put( getKey( x, y, z ), val );
	}
	
	public T get( int x, int y, int z )
	{
		return get( getKey( x, y, z ) );
	}
	
	public T remove( int x, int y, int z )
	{
		return remove( getKey( x, y, z ) );
	}
	
	private int getKey( int x, int y, int z )
	{
		return Bits.packSignedToInt( x, XSize, XOffset )
			| Bits.packSignedToInt( y, YSize, YOffset )
			| Bits.packSignedToInt( z, ZSize, ZOffset );
	}
	
	public int getKeyX( int key )
	{
		return Bits.unpackSigned( key, XSize, XOffset );
	}
	
	public int getKeyY( int key )
	{
		return Bits.unpackSigned( key, YSize, YOffset );
	}
	
	public int getKeyZ( int key )
	{
		return Bits.unpackSigned( key, ZSize, ZOffset );
	}
}
