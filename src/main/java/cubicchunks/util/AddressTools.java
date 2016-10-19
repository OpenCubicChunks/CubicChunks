/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.util;

// TODO: remove this... packing x, y, z into a long is just stupid.
//       Mojang did it with x, z and it was stupid than also!
public class AddressTools {
	// Anvil format details:
	// within a region file, each chunk coord gets 5 bits
	// the coord for each region is capped at 27 bits

	// here's the encoding scheme for 64 bits of space:
	// y:         20 bits, signed,   1,048,576 chunks, 16,777,216 blocks
	// x:         22 bits, signed,   4,194,304 chunks, 67,108,864 blocks
	// z:         22 bits, signed,   4,194,304 chunks, 67,108,864 blocks

	// World.setBlock() caps block x,z at -30m to 30m, so our 22-bit cap on chunk x,z is just right!

	// the Anvil format gives 32 bits to each chunk coordinate, but we're only giving 22 bits
	// moving at 8 blocks/s (which is like minecart speed), it would take ~48 days to reach the x or z edge from the center
	// at the same speed, it would take ~24 days to reach the top from the bottom
	// that seems like enough room for a minecraft world

	// 0         1        |2         3         4|        5         6  |
	// 0123456789012345678901234567890123456789012345678901234567890123
	// yyyyyyyyyyyyyyyyyyyyxxxxxxxxxxxxxxxxxxxxxxzzzzzzzzzzzzzzzzzzzzzz

	private static final int Y_BITS = 20;
	private static final int X_BITS = 22;
	private static final int Z_BITS = 22;

	private static final int Z_BIT_OFFSET = 0;
	private static final int X_BIT_OFFSET = Z_BIT_OFFSET + Z_BITS;
	private static final int Y_BIT_OFFSET = X_BIT_OFFSET + X_BITS;

	public static final int MIN_CUBE_Y = Bits.getMinSigned(Y_BITS);
	public static final int MAX_CUBE_Y = Bits.getMaxSigned(Y_BITS);
	public static final int MIN_CUBE_X = Bits.getMinSigned(X_BITS);
	public static final int MAX_CUBE_X = Bits.getMaxSigned(X_BITS);
	public static final int MIN_CUBE_Z = Bits.getMinSigned(Z_BITS);
	public static final int MAX_CUBE_Z = Bits.getMaxSigned(Z_BITS);

	public static final int MIN_BLOCK_Y = Coords.cubeToMinBlock(MIN_CUBE_Y);
	public static final int MAX_BLOCK_Y = Coords.cubeToMaxBlock(MAX_CUBE_Y);

	public static long getAddress(int x, int y, int z) {
		return Bits.packSignedToLong(y, Y_BITS, Y_BIT_OFFSET)
				| Bits.packSignedToLong(x, X_BITS, X_BIT_OFFSET)
				| Bits.packSignedToLong(z, Z_BITS, Z_BIT_OFFSET);
	}

	public static long getAddress(int x, int z) {
		return Bits.packSignedToLong(x, X_BITS, X_BIT_OFFSET)
				| Bits.packSignedToLong(z, Z_BITS, Z_BIT_OFFSET);
	}

	public static int getY(long address) {
		return Bits.unpackSigned(address, Y_BITS, Y_BIT_OFFSET);
	}

	public static int getX(long address) {
		return Bits.unpackSigned(address, X_BITS, X_BIT_OFFSET);
	}

	public static int getZ(long address) {
		return Bits.unpackSigned(address, Z_BITS, Z_BIT_OFFSET);
	}

	public static short getLocalAddress(int localX, int localY, int localZ) {
		return (short) (Bits.packUnsignedToInt(localX, 4, 0)
						| Bits.packUnsignedToInt(localY, 4, 4)
						| Bits.packUnsignedToInt(localZ, 4, 8));
	}

	public static int getLocalX(int localAddress) {
		return Bits.unpackUnsigned(localAddress, 4, 0);
	}

	public static int getLocalY(int localAddress) {
		return Bits.unpackUnsigned(localAddress, 4, 4);
	}

	public static int getLocalZ(int localAddress) {
		return Bits.unpackUnsigned(localAddress, 4, 8);
	}
}
