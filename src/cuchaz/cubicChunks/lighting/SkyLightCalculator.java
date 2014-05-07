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
package cuchaz.cubicChunks.lighting;

import net.minecraft.world.EnumSkyBlock;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public class SkyLightCalculator extends ColumnCalculator
{
	@Override
	public boolean calculate( Column column )
	{
		// NOTE: this is called right after chunk generation, and right after any new segments are created
		
		// UNDONE: move rain calculations out of the lighting system!
		
		// init the rain map to -999, which is a kind of null value
		// this array is actually a cache
		// values will be calculated by the getter
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				column.precipitationHeightMap[localX + (localZ << 4)] = -999;
			}
		}
		
		// no sky? no sky light
		if( column.worldObj.provider.hasNoSky )
		{
			return true;
		}
		
		// build the skylight map
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				calculateBlockColumn( column, localX, localZ );
			}
		}
		
		return true;
	}
	
	private void calculateBlockColumn( Column column, int localX, int localZ )
	{
		int maxBlockY = Coords.cubeToMaxBlock( column.getTopCubeY() ) + 1;
		int minBlockY = Coords.cubeToMinBlock( column.getBottomCubeY() );
		
		// start with full light for this block
		int lightValue = 15;
		
		// start with the top block and fall down
		for( int blockY=maxBlockY; blockY>=minBlockY; blockY-- )
		{
			// light opacity is [0,255], all blocks 0, 255 except ice,water:3, web:1
			int lightOpacity = column.func_150808_b( localX, blockY, localZ );
			if( lightOpacity == 0 && lightValue != 15 )
			{
				// after something blocks light, apply a linear falloff
				lightOpacity = 1;
			}
			
			// decrease the light
			lightValue -= lightOpacity;
			
			// stop when we run out of light
			if( lightValue <= 0 )
			{
				break;
			}
			
			// update the cube only if it's actually loaded
			Cube cube = column.getCube( Coords.blockToCube( blockY ) );
			if( cube != null )
			{
				// save the sky light value
				int localY = Coords.blockToLocal( blockY );
				cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, lightValue );
				
				// signal a render update
				int blockX = Coords.localToBlock( column.xPosition, localX );
				int blockZ = Coords.localToBlock( column.zPosition, localZ );
				column.worldObj.func_147479_m( blockX, blockY, blockZ );
			}
		}
	}
}
