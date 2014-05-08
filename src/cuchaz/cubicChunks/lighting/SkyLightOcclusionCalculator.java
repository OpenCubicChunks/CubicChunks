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
package cuchaz.cubicChunks.lighting;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.world.Column;

public class SkyLightOcclusionCalculator extends BlockColumnCalculator
{
	@Override
	public boolean calculate( Column column, int localX, int localZ, int blockX, int blockZ )
	{
		World world = column.worldObj;
		
		// get the height
		// NOTE: height here is the value of the first air block on top of the column
		int height = column.getHeightValue( localX, localZ );
		
		// get the min column height among neighbors
		int minHeight1 = world.getChunkHeightMapMinimum( blockX - 1, blockZ );
		int minHeight2 = world.getChunkHeightMapMinimum( blockX + 1, blockZ );
		int minHeight3 = world.getChunkHeightMapMinimum( blockX, blockZ - 1 );
		int minHeight4 = world.getChunkHeightMapMinimum( blockX, blockZ + 1 );
		int minHeight = Math.min( minHeight1, Math.min( minHeight2, Math.min( minHeight3, minHeight4 ) ) );
		
		boolean actuallyUpdated = false;
		actuallyUpdated |= checkSkylightNeighborHeight( world, blockX, blockZ, minHeight );
		actuallyUpdated |= checkSkylightNeighborHeight( world, blockX - 1, blockZ, height );
		actuallyUpdated |= checkSkylightNeighborHeight( world, blockX + 1, blockZ, height );
		actuallyUpdated |= checkSkylightNeighborHeight( world, blockX, blockZ - 1, height );
		actuallyUpdated |= checkSkylightNeighborHeight( world, blockX, blockZ + 1, height );
		
		if( actuallyUpdated )
		{
			column.isModified = true;
		}
		
		return true;
	}
	
	private boolean checkSkylightNeighborHeight( World world, int blockX, int blockZ, int maxBlockY )
	{
		int height = world.getHeightValue( blockX, blockZ );
		if( height > maxBlockY )
		{
			return updateSkylightNeighborHeight( world, blockX, blockZ, maxBlockY, height );
		}
		else if( height < maxBlockY )
		{
			return updateSkylightNeighborHeight( world, blockX, blockZ, height, maxBlockY );
		}
		
		return false;
	}
	
	private boolean updateSkylightNeighborHeight( World world, int blockX, int blockZ, int minBlockY, int maxBlockY )
	{
		if( maxBlockY <= minBlockY )
		{
			return false;
		}
		
		if( !world.doChunksNearChunkExist( blockX, 0, blockZ, 16 ) )
		{
			return false;
		}
		
		CubeWorld cubeWorld = (CubeWorld)world;
		for( int blockY=minBlockY; blockY<=maxBlockY; blockY++ )
		{
			cubeWorld.getLightingManager().computeDiffuseLighting( blockX, blockY, blockZ, EnumSkyBlock.Sky );
		}
		
		return true;
	}
}
