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

public class Bits
{
	public static long packUnsignedToLong( int unsigned, int size, int offset )
	{
		// same as signed
		return packSignedToLong( unsigned, size, offset );
	}
	
	public static long packSignedToLong( int signed, int size, int offset )
	{
		long result = signed & getMask( size );
		return result << offset;
	}
	
	public static int packUnsignedToInt( int unsigned, int size, int offset )
	{
		// same as signed
		return packSignedToInt( unsigned, size, offset );
	}
	
	public static int packSignedToInt( int signed, int size, int offset )
	{
		int result = signed & getMask( size );
		return result << offset;
	}
	
	public static int unpackUnsigned( long packed, int size, int offset )
	{
		packed = packed >> offset;
		return (int)packed & getMask( size );
	}
	
	public static int unpackSigned( long packed, int size, int offset )
	{
		// first, offset to the far left and back so we can preserve the two's complement
		int complementOffset = 64 - offset - size;
		packed = packed << complementOffset >> complementOffset;
		
		// then unpack the integer
		packed = packed >> offset;
		return (int)packed;
	}
	
	public static int unpackUnsigned( int packed, int size, int offset )
	{
		packed = packed >> offset;
		return (int)packed & getMask( size );
	}
	
	public static int unpackSigned( int packed, int size, int offset )
	{
		// first, offset to the far left and back so we can preserve the two's complement
		int complementOffset = 64 - offset - size;
		packed = packed << complementOffset >> complementOffset;
		
		// then unpack the integer
		packed = packed >> offset;
		return (int)packed;
	}
	
	public static int getMask( int size )
	{
		// mask sizes of 0 and 32 are not allowed
		// we could allow them, but I don't want to add conditionals so this method stays very fast
		assert( size > 0 && size < 32 );
		return 0xffffffff >>> ( 32 - size );
	}
	
	public static int getMinSigned( int size )
	{
		return -( 1 << ( size - 1 ) );
	}
	
	public static int getMaxSigned( int size )
	{
		return ( 1 << ( size - 1 ) ) - 1;
	}
	
	public static int getMaxUnsigned( int size )
	{
		return ( 1 << size ) - 1;
	}
}
