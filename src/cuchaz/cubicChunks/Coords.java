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
		return ( chunk << 4 ) + local;
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
		// entities stand on blocks
		// the entity (on top of the block) might actually be in one cubic chunk,
		// but the block could be in one cubic chunk below.
		// so for entities standing on blocks, assign them to the chunk of the block below
		return blockToChunk( MathHelper.floor_double( entity.posY - 1 ) );
	}
}
