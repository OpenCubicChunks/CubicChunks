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
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.util.BlockColumnProcessor;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;

public class SkyLightOcclusionProcessor extends BlockColumnProcessor
{
	public SkyLightOcclusionProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public boolean calculate( Column column, int localX, int localZ, int blockX, int blockZ )
	{
		World world = column.worldObj;
		
		// get the height
		Integer height = column.getSkylightBlockY( localX, localZ );
		if( height == null )
		{
			// nothing to do
			return true;
		}
		
		// get the min column height among neighbors
		int minHeight1 = world.getChunkHeightMapMinimum( blockX - 1, blockZ );
		int minHeight2 = world.getChunkHeightMapMinimum( blockX + 1, blockZ );
		int minHeight3 = world.getChunkHeightMapMinimum( blockX, blockZ - 1 );
		int minHeight4 = world.getChunkHeightMapMinimum( blockX, blockZ + 1 );
		int minNeighborHeight = Math.min( minHeight1, Math.min( minHeight2, Math.min( minHeight3, minHeight4 ) ) );
		
		boolean actuallyUpdated = false;
		actuallyUpdated |= updateSkylight( world, blockX, blockZ, minNeighborHeight );
		actuallyUpdated |= updateSkylight( world, blockX - 1, blockZ, height );
		actuallyUpdated |= updateSkylight( world, blockX + 1, blockZ, height );
		actuallyUpdated |= updateSkylight( world, blockX, blockZ - 1, height );
		actuallyUpdated |= updateSkylight( world, blockX, blockZ + 1, height );
		
		if( actuallyUpdated )
		{
			column.isModified = true;
		}
		
		return true;
	}
	
	private boolean updateSkylight( World world, int blockX, int blockZ, int maxBlockY )
	{
		// get the skylight block for this block column
		Column column = (Column)world.getChunkFromBlockCoords( blockX, blockZ );
		int localX = Coords.blockToLocal( blockX );
		int localZ = Coords.blockToLocal( blockZ );
		Integer height = column.getSkylightBlockY( localX, localZ );
		if( height == null )
		{
			// nothing to do
			return false;
		}
		
		if( height > maxBlockY )
		{
			return updateSkylight( world, blockX, blockZ, maxBlockY, height );
		}
		else if( height < maxBlockY )
		{
			return updateSkylight( world, blockX, blockZ, height, maxBlockY );
		}
		
		return false;
	}
	
	private boolean updateSkylight( World world, int blockX, int blockZ, int minBlockY, int maxBlockY )
	{
		if( maxBlockY <= minBlockY )
		{
			return false;
		}
		
		if( !world.doChunksNearChunkExist( blockX, minBlockY, blockZ, maxBlockY ) )
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
