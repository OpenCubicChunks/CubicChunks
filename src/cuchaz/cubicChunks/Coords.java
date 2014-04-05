/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class Coords
{
	public static int blockToLocal( int val )
	{
		return val & 0xf;
	}
	
	public static int blockToChunk( int val )
	{
		return val >> 4;
	}
	
	public static int localToBlock( int chunk, int local )
	{
		return chunkToMinBlock( chunk ) + local;
	}
	
	public static int chunkToMinBlock( int chunk )
	{
		return chunk << 4;
	}
	
	public static int getChunkXForEntity( Entity entity )
	{
		return blockToChunk( MathHelper.floor_double( entity.posX ) );
	}
	
	public static int getChunkZForEntity( Entity entity )
	{
		return blockToChunk( MathHelper.floor_double( entity.posZ ) );
	}
	
	public static int getChunkYForEntity( Entity entity )
	{
		// the entity is in the cubic chunk it's inside, not the cubic chunk it's standing on
		return blockToChunk( MathHelper.floor_double( entity.posY ) );
	}
}
