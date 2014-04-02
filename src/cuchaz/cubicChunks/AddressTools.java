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

import net.minecraft.world.chunk.Chunk;

public class AddressTools
{
	// here's the encoding scheme
	// we're aiming for a 48-bit integer
	
	// dimension: 4 bits, 16 dimensions
	// y: 12 bits,  4096 chunks,   65536 blocks
	// x: 16 bits, 65536 chunks, 1048576 blocks
	// z: 16 bits, 65536 chunks, 1048576 blocks
	
	// moving at 8 blocks/s (which is like minecart speed), it would take ~36.4 hours to cross this distance
	// that seems like enough room for a minecraft world
	
	// 0         1         2         3|        4      |  5         6  |
	// 0123456789012345678901234567890123456789012345678901234567890123
	// ddddyyyyyyyyyyyyxxxxxxxxxxxxxxxxzzzzzzzzzzzzzzzz
	
	private static final int DimensionSize = 4;
	private static final int YSize = 12;
	private static final int XSize = 16;
	private static final int ZSize = 16;
	
	private static final int ZOffset = 0;
	private static final int XOffset = ZOffset + ZSize;
	private static final int YOffset = XOffset + XSize;
	private static final int DimensionOffset = YOffset + YSize;
	
	public static final int MinDimension = 0;
	public static final int MaxDimension = Bits.getMaxUnsigned( DimensionSize );
	public static final int MinY = Bits.getMinSigned( YSize );
	public static final int MaxY = Bits.getMaxSigned( YSize );
	public static final int MinX = Bits.getMinSigned( XSize );
	public static final int MaxX = Bits.getMaxSigned( XSize );
	public static final int MinZ = Bits.getMinSigned( ZSize );
	public static final int MaxZ = Bits.getMaxSigned( ZSize );
	
	public static long toAddress( Chunk chunk )
	{
		return toAddress(
			chunk.worldObj.provider.dimensionId,
			chunk.xPosition,
			0,
			chunk.zPosition
		);
	}
	
	public static long toAddress( int dimension, int x, int y, int z )
	{
		return Bits.packUnsigned( dimension, DimensionSize, DimensionOffset )
			| Bits.packUnsigned( y, YSize, YOffset )
			| Bits.packUnsigned( x, XSize, XOffset )
			| Bits.packUnsigned( z, ZSize, ZOffset );
	}
	
	public static int getDimension( long address )
	{
		return Bits.unpackUnsigned( address, DimensionSize, DimensionOffset );
	}
	
	public static int getY( long address )
	{
		return Bits.unpackSigned( address, YSize, YOffset );
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
