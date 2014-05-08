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

import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public class SkyLightUpdateCalculator
{
	public void calculate( Column column, int localX, int localZ, int minBlockY, int maxBlockY )
	{
		// NOTE: maxBlockY is always the air block above the top block that was added or removed
		
		World world = column.worldObj;
		LightingManager lightingManager = ((CubeWorld)world).getLightingManager();
		
		if( world.provider.hasNoSky )
		{
			return;
		}
		
		// did we add or remove sky?
		boolean addedSky = column.func_150810_a( localX, maxBlockY-1, localZ ) == Blocks.air;
		int newMaxBlockY = addedSky ? minBlockY : maxBlockY;
		
		// reset sky light for the affected y range
		int light = addedSky ? 15 : 0;
		for( int blockY=minBlockY; blockY<maxBlockY; blockY++ )
		{
			// save the light value
			int cubeY = Coords.blockToCube( blockY );
			Cube cube = column.getCube( cubeY );
			if( cube != null )
			{
				int localY = Coords.blockToLocal( blockY );
				cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, light );
			}
		}
		
		// compute the skylight falloff starting at the new top block
		light = 15;
		for( int blockY=newMaxBlockY-1; blockY>0; blockY-- )
		{
			// get the opacity to apply for this block
			int lightOpacity = Math.max( 1, column.func_150808_b( localX, blockY, localZ ) );
			
			// compute the falloff
			light = Math.max( light - lightOpacity, 0 );
			
			// save the light value
			int cubeY = Coords.blockToCube( blockY );
			Cube cube = column.getCube( cubeY );
			if( cube != null )
			{
				int localY = Coords.blockToLocal( blockY );
				cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, light );
			}
			
			if( light == 0 )
			{
				// we ran out of light
				break;
			}
		}
		
		// update this block and its xz neighbors
		int blockX = Coords.localToBlock( column.xPosition, localX );
		int blockZ = Coords.localToBlock( column.zPosition, localZ );
		diffuseSkyLightForBlockColumn( lightingManager, blockX - 1, blockZ, minBlockY, maxBlockY );
		diffuseSkyLightForBlockColumn( lightingManager, blockX + 1, blockZ, minBlockY, maxBlockY );
		diffuseSkyLightForBlockColumn( lightingManager, blockX, blockZ - 1, minBlockY, maxBlockY );
		diffuseSkyLightForBlockColumn( lightingManager, blockX, blockZ + 1, minBlockY, maxBlockY );
		diffuseSkyLightForBlockColumn( lightingManager, blockX, blockZ, minBlockY, maxBlockY );
	}
	
	private void diffuseSkyLightForBlockColumn( LightingManager lightingManager, int blockX, int blockZ, int minBlockY, int maxBlockY )
	{
		for( int blockY=minBlockY; blockY<maxBlockY; blockY++ )
		{
			lightingManager.computeDiffuseLighting( blockX, blockY, blockZ, EnumSkyBlock.Sky );
		}
	}
}
