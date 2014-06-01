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
	
public class Coords
{	
	public static final int CUBE_MAX_X = 16;
	public static final int CUBE_MAX_Y = 16;
	public static final int CUBE_MAX_Z = 16;
	
	public static final int HALF_CUBE_MAX_X = CUBE_MAX_X / 2;
	public static final int HALF_CUBE_MAX_Y = CUBE_MAX_Y / 2;
	public static final int HALF_CUBE_MAX_Z = CUBE_MAX_Z / 2;
	
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
