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
package cuchaz.cubicChunks.util;

public class CubeAddress
{
	// Anvil format details:
	// within a region file, each chunk coord gets 5 bits
	// the coord for each region is capped at 27 bits
	
	// World.setBlock() caps block x,z at -30m to 30m, so our 22-bit cap on chunk x,z is just right!
	
	// here's the encoding scheme for 64 bits of space:
	// y:         20 bits, signed,   1,048,576 chunks, 16,777,216 blocks
	// x:         22 bits, signed,   4,194,304 chunks, 67,108,864 blocks
	// z:         22 bits, signed,   4,194,304 chunks, 67,108,864 blocks
	
	// the Anvil format gives 32 bits to each chunk coordinate, but we're only giving 22 bits
	// moving at 8 blocks/s (which is like minecart speed), it would take ~48 days to reach the x or z edge from the center
	// at the same speed, it would take ~24 days to reach the top from the bottom
	// that seems like enough room for a minecraft world
	
	// 0         1         2         3|        4      |  5         6  |
	// 0123456789012345678901234567890123456789012345678901234567890123
	// yyyyyyyyyyyyyyyyyyyyxxxxxxxxxxxxxxxxxxxxxxzzzzzzzzzzzzzzzzzzzzzz
	
	private static final int YSize = 20;
	private static final int XSize = 22;
	private static final int ZSize = 22;
	
	private static final int ZOffset = 0;
	private static final int XOffset = ZOffset + ZSize;
	private static final int YOffset = XOffset + XSize;
	
	public static final int MinY = Bits.getMinSigned( YSize );
	public static final int MaxY = Bits.getMaxSigned( YSize );
	public static final int MinX = Bits.getMinSigned( XSize );
	public static final int MaxX = Bits.getMaxSigned( XSize );
	public static final int MinZ = Bits.getMinSigned( ZSize );
	public static final int MaxZ = Bits.getMaxSigned( ZSize );
	
	public static long getAddress( int x, int y, int z )
	{
		return Bits.packSignedToLong( y, YSize, YOffset )
			| Bits.packSignedToLong( x, XSize, XOffset )
			| Bits.packSignedToLong( z, ZSize, ZOffset );
	}
	
	public static long getAddress( int x, int z )
	{
		return Bits.packSignedToLong( x, XSize, XOffset )
			| Bits.packSignedToLong( z, ZSize, ZOffset );
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
	
	/**
	 * Position of a cube.
	 * <p>
	 * Tall Worlds uses a column coordinate system (which is really just a cube 
	 * coordinate system without the y-coordinate), a cube coordinate system,
	 * and two block coordinate systems, a cube-relative system, and a world absolute
	 * system.
	 * <p>
	 * It is important that the systems are kept separate. This class should be 
	 * used whenever a cube coordinate is passed along, so that it is clear that
	 * cube coordinates are being used, and not block coordinates.
	 * <p>
	 * Additionally, I (Nick) like to use xRel, yRel, and zRel for the relative
	 * position of a block inside of a cube. In world space, I (Nick) refer to the
	 * coordinates as xAbs, yAbs, and zAbs.
	 * <p>
	 * See {@link CubeAddress} for details of hashing the cube coordinates for keys and 
	 * storage.
	 * <p>
	 * This class also contains some helper methods to switch from/to block
	 * coordinates.
	 */
	private static final int CUBE_MAX_X = 16;
	private static final int CUBE_MAX_Y = 16;
	private static final int CUBE_MAX_Z = 16;
	
	private static final int HALF_CUBE_MAX_X = CUBE_MAX_X / 2;
	private static final int HALF_CUBE_MAX_Y = CUBE_MAX_Y / 2;
	private static final int HALF_CUBE_MAX_Z = CUBE_MAX_Z / 2;
	
	private final int cubeX;
	private final int cubeY;
	private final int cubeZ;
	
	protected CubeAddress( int cubeX, int cubeY, int cubeZ)
	{
		this.cubeX = cubeX;
		this.cubeY = cubeY;
		this.cubeZ = cubeZ;
	}
	
	/**
	 * Gets the x position of the cube in the world.
	 * 
	 * @return The x position.
	 */
	public int getCubeX()
	{
		return this.cubeX;
	}
	
	/**
	 * Gets the y position of the cube in the world.
	 * 
	 * @return The y position.
	 */
	public int getCubeY()
	{
		return this.cubeY;
	}
	
	/**
	 * Gets the z position of the cube in the world.
	 * 
	 * @return The z position.
	 */
	public int getCubeZ()
	{
		return this.cubeZ;
	}
	
	/**
	 * Gets the coordinates of the cube as a string.
	 * 
	 * @return The coordinates, formatted as a string.
	 */
	@Override
	public String toString()
	{
		return this.cubeX + "," + this.cubeY + "," + this.cubeZ;
	}
	
	/**
	 * Compares the CubeCoordinate against the given object.
	 * 
	 * @return True if the cube matches the given object, but false if it
	 * doesn't match, or is null, or not a CubeCoordinate object.
	 */
	@Override
	public boolean equals(Object otherObject)
	{
		if (otherObject == this)
        {
            return true;
        }
		
        if (otherObject == null)
        {
            return false;
        }
        
        if (!(otherObject instanceof Coords))
        {
            return false;
        }
        
        CubeAddress otherCubeCoordinate = (CubeAddress) otherObject;
        
        if (otherCubeCoordinate.cubeX != cubeX)
        {
            return false;
        }
        
        if (otherCubeCoordinate.cubeY != cubeY)
        {
            return false;
        }
        
        if (otherCubeCoordinate.cubeZ != cubeZ)
        {
            return false;
        }
        
        return true;
	}
	
	/**
	 * Gets the absolute position of the cube's center on the x axis.
	 * 
	 * @return The x center of the cube.
	 */
	public int getXCenter()
	{
		return cubeX * CUBE_MAX_X + HALF_CUBE_MAX_X;
	}
	
	/**
	 * Gets the absolute position of the cube's center on the y axis.
	 * 
	 * @return The y center of the cube.
	 */
	public int getYCenter()
	{
		return cubeY * CUBE_MAX_Y + HALF_CUBE_MAX_Y;
	}
	
	/**
	 * Gets the absolute position of the cube's center on the z axis.
	 * 
	 * @return The z center of the cube.
	 */
	public int getZCenter()
	{
		return cubeZ * CUBE_MAX_Z + HALF_CUBE_MAX_Z;
	}
	
	public int getMinBlockX()
	{
		return Coords.cubeToMinBlock(cubeX);
	}
	
	public int getMinBlockY()
	{
		return Coords.cubeToMinBlock(cubeX);
	}
	
	public int getMinBlockZ()
	{
		return Coords.cubeToMinBlock(cubeX);
	}
}
