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
	// Anvil format details:
	// within a region file, each chunk coord gets 5 bits
	// the coord for each region is capped at 27 bits
	
	// here's the encoding scheme for 64 bits of space:
	// dimension:  8 bits, signed,   256 dimensions
	// y:         12 bits, unsigned,   4,096 chunks,     65,536 blocks
	// x:         22 bits, signed, 4,194,304 chunks, 67,108,864 blocks
	// z:         22 bits, signed, 4,194,304 chunks, 67,108,864 blocks
	
	// the Anvil format gives 32 bits to each chunk coordinate, but we're only giving 22 bits
	// moving at 8 blocks/s (which is like minecart speed), it would take ~48 days to reach the x or z edge from the center
	// at the same speed, it would take ~1.3 hours to reach the bottom from the center
	// that seems like enough room for a minecraft world
	
	// 0         1         2         3|        4      |  5         6  |
	// 0123456789012345678901234567890123456789012345678901234567890123
	// ddddddddyyyyyyyyyyyyxxxxxxxxxxxxxxxxxxxxxxzzzzzzzzzzzzzzzzzzzzzz
	
	private static final int DimensionSize = 8;
	private static final int YSize = 12;
	private static final int XSize = 22;
	private static final int ZSize = 22;
	
	private static final int ZOffset = 0;
	private static final int XOffset = ZOffset + ZSize;
	private static final int YOffset = XOffset + XSize;
	private static final int DimensionOffset = YOffset + YSize;
	
	public static final int MinDimension = Bits.getMinSigned( DimensionSize );
	public static final int MaxDimension = Bits.getMaxSigned( DimensionSize );
	public static final int MinY = Bits.getMinSigned( YSize );
	public static final int MaxY = Bits.getMaxSigned( YSize );
	public static final int MinX = Bits.getMinSigned( XSize );
	public static final int MaxX = Bits.getMaxSigned( XSize );
	public static final int MinZ = Bits.getMinSigned( ZSize );
	public static final int MaxZ = Bits.getMaxSigned( ZSize );
	
	public static long getAddress( int dimension, int x, int y, int z )
	{
		return Bits.packSignedToLong( dimension, DimensionSize, DimensionOffset )
			| Bits.packUnsignedToLong( y, YSize, YOffset )
			| Bits.packSignedToLong( x, XSize, XOffset )
			| Bits.packSignedToLong( z, ZSize, ZOffset );
	}
	
	public static int getDimension( long address )
	{
		return Bits.unpackSigned( address, DimensionSize, DimensionOffset );
	}
	
	public static int getY( long address )
	{
		return Bits.unpackUnsigned( address, YSize, YOffset );
	}
	
	public static int getX( long address )
	{
		return Bits.unpackSigned( address, XSize, XOffset );
	}
	
	public static int getZ( long address )
	{
		return Bits.unpackSigned( address, ZSize, ZOffset );
	}
}
