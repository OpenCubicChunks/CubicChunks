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

public class AddressTools
{
	public static long toAddress( int dimension, int x, int y, int z )
	{
		// here's the encoding scheme
		// we're aiming for a 48-bit integer
		
		// MSB is reserved so longs are always > 0 (stupid lack of unsigned int support in java...)
		// dimension: 3 bits, 7 dimensions (it would be 8, but all-0 addresses are not allowed either)
		// y: 12 bits,  4096 chunks,   65536 blocks
		// x: 16 bits, 65536 chunks, 1048576 blocks
		// z: 16 bits, 65536 chunks, 1048576 blocks
		
		// moving at 8 blocks/s (which is like minecart speed), it would take ~36.4 hours to cross this distance
		// that seems like enough room for a minecraft world
		
		// 0         1         2         3|        4      |  5         6  |
		// 0123456789012345678901234567890123456789012345678901234567890123
		// 0dddyyyyyyyyyyyyxxxxxxxxxxxxxxxxzzzzzzzzzzzzzzzz
		
		long address = ( ( (dimension+1) & 0x7 ) << 44 ) | ( ( y & 0xfff ) << 32 ) | ( ( x & 0xffff ) << 16 ) + ( z & 0xffff );
		assert( address > 0 );
		return address;
	}
	
	public static int getDimension( long address )
	{
		return ((int)( address >> 44 ) & 0xff ) - 1;
	}
	
	public static int getY( long address )
	{
		return (int)( address >> 32 ) & 0xfff;
	}
	
	public static int getX( long address )
	{
		return (int)( address >> 16 ) & 0xffff;
	}
	
	public static int getZ( long address )
	{
		return (int)( address ) & 0xffff;
	}
}
