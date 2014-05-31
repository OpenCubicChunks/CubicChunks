/*******************************************************************************
 * Copyright (c) 2014 Jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin- initial API and implementation
 *     Nick Whitney - expanded to CubeCoordinate
 ******************************************************************************/
package cuchaz.cubicChunks.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

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
 * See {@link AddressTools} for details of hashing the cube coordinates for keys and 
 * storage.
 * <p>
 * This class also contains some helper methods to switch from/to block
 * coordinates.
 */
public class CubeCoordinate
{
	private static final int CUBE_MAX_X = 16;
	private static final int CUBE_MAX_Y = 16;
	private static final int CUBE_MAX_Z = 16;
	
	private static final int HALF_CUBE_MAX_X = CUBE_MAX_X / 2;
	private static final int HALF_CUBE_MAX_Y = CUBE_MAX_Y / 2;
	private static final int HALF_CUBE_MAX_Z = CUBE_MAX_Z / 2;
	
	private final int cubeX;
	private final int cubeY;
	private final int cubeZ;
	
	protected CubeCoordinate( int cubeX, int cubeY, int cubeZ)
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
	public int getCubeT()
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
        
        if (!(otherObject instanceof CubeCoordinate))
        {
            return false;
        }
        
        CubeCoordinate otherCubeCoordinate = (CubeCoordinate) otherObject;
        
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
		return cubeToMinBlock(cubeX);
	}
	
	public int getMinBlockY()
	{
		return cubeToMinBlock(cubeX);
	}
	
	public int getMinBlockZ()
	{
		return cubeToMinBlock(cubeX);
	}
	
// Helper functions	
	
	public static int blockToLocal( int val )
	{
		return val & 0xf;
	}
	
	public static int blockToCube( int val )
	{
		return val >> 4;
	}
	
	public static int localToBlock( int cubeVal, int localVal )
	{
		return cubeToMinBlock( cubeVal ) + localVal;
	}
	
	public static int cubeToMinBlock( int val )
	{
		return val << 4;
	}
	
	public static int cubeToMaxBlock( int val )
	{
		return cubeToMinBlock( val ) + 15;
	}
	
	public static int getCubeXForEntity( Entity entity )
	{
		return blockToCube( MathHelper.floor_double( entity.posX ) );
	}
	
	public static int getCubeZForEntity( Entity entity )
	{
		return blockToCube( MathHelper.floor_double( entity.posZ ) );
	}
	
	public static int getCubeYForEntity( Entity entity )
	{
		// the entity is in the cube it's inside, not the cube it's standing on
		return blockToCube( MathHelper.floor_double( entity.posY ) );
	}
}
