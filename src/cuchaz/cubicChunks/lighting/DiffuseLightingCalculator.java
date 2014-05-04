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

import net.minecraft.block.Block;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

public class DiffuseLightingCalculator
{
	private int[] m_lightUpdateBlockList;
	
	public DiffuseLightingCalculator( )
	{
		m_lightUpdateBlockList = new int[32768];
	}
	
	public boolean calculate( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		// TEMP: just light the source block
		world.setLightValue( lightType, blockX, blockY, blockZ, 15 );
		
		return true;
	}
	
	public boolean calculateReal( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		// are there enough nearby blocks to do the lighting?
		if( !world.doChunksNearChunkExist( blockX, blockY, blockZ, 17 ) )
		{
			return false;
		}
		
		int i = 0;
		int updateCounter = 0;
		world.theProfiler.startSection( "getBrightness" );
		int oldLight = world.getSavedLightValue( lightType, blockX, blockY, blockZ );
		int newLight = computeLightValue( world, blockX, blockY, blockZ, lightType );
		int updateData;
		int updateBlockX;
		int updateBlockY;
		int updateBlockZ;
		int updateLight;
		int updateOldLight;
		int blockDx;
		int blockDy;
		int blockDz;
		
		if( newLight > oldLight )
		{
			// queue an update at this block for 0
			m_lightUpdateBlockList[updateCounter++] = 133152; // ( light=0, x=0, y=0, z=0 )
		}
		else if( newLight < oldLight )
		{
			// remove light from the world
			
			// queue an update at this block for the old light
			m_lightUpdateBlockList[updateCounter++] = 133152 | oldLight << 18;
			
			// for each queued light update...
			while( i < updateCounter )
			{
				updateData = m_lightUpdateBlockList[i++];
				updateBlockX = ( updateData & 63 ) - 32 + blockX;
				updateBlockY = ( updateData >> 6 & 63 ) - 32 + blockY;
				updateBlockZ = ( updateData >> 12 & 63 ) - 32 + blockZ;
				updateLight = updateData >> 18 & 15;
				updateOldLight = world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ );
				
				if( updateOldLight == updateLight )
				{
					// set current light to 0
					world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, 0 );
					
					if( updateLight > 0 )
					{
						// only look at nearby updates
						blockDx = MathHelper.abs_int( updateBlockX - blockX );
						blockDy = MathHelper.abs_int( updateBlockY - blockY );
						blockDz = MathHelper.abs_int( updateBlockZ - blockZ );
						if( blockDx + blockDy + blockDz < 17 )
						{
							// for each face-neighboring block...
							for( int side = 0; side < 6; ++side )
							{
								int neighborBlockX = updateBlockX + Facing.offsetsXForSide[side];
								int neighborBlockY = updateBlockY + Facing.offsetsYForSide[side];
								int neighborBlockZ = updateBlockZ + Facing.offsetsZForSide[side];
								int neighborOpacityMin1 = Math.max( 1, world.getBlock( neighborBlockX, neighborBlockY, neighborBlockZ ).getLightOpacity() );
								int neighborOldLight = world.getSavedLightValue( lightType, neighborBlockX, neighborBlockY, neighborBlockZ );
								
								if( neighborOldLight == updateLight - neighborOpacityMin1 && updateCounter < m_lightUpdateBlockList.length )
								{
									// queue an update for the neighbor block with the value the neighbor light should be
									m_lightUpdateBlockList[updateCounter++] =
										neighborBlockX - blockX + 32
										| neighborBlockY - blockY + 32 << 6
										| neighborBlockZ - blockZ + 32 << 12
										| updateLight - neighborOpacityMin1 << 18;
								}
							}
						}
					}
				}
			}
			
			i = 0;
		}
		
		world.theProfiler.endSection();
		world.theProfiler.startSection( "checkedPosition < toCheckCount" );
		
		// for each update...
		while( i < updateCounter )
		{
			updateData = m_lightUpdateBlockList[i++];
			updateBlockX = ( updateData & 63 ) - 32 + blockX;
			updateBlockY = ( updateData >> 6 & 63 ) - 32 + blockY;
			updateBlockZ = ( updateData >> 12 & 63 ) - 32 + blockZ;
			updateOldLight = world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ );
			int computedLight = computeLightValue( world, updateBlockX, updateBlockY, updateBlockZ, lightType );
			
			// TEMP: is light even saveable here?
			int testLightValue = 5;
			if( updateOldLight == testLightValue )
			{
				testLightValue = 6;
			}
			world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, testLightValue );
			boolean isLightSavableHere = world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ ) == testLightValue;
			world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, updateOldLight );
			if( !isLightSavableHere )
			{
				continue;
			}
			
			// if the light at this update block needs to change
			if( computedLight != updateOldLight )
			{
				// change it
				world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, computedLight );
				
				// if we got brighter
				if( computedLight > updateOldLight )
				{
					// only look at nearby updates
					blockDx = Math.abs( updateBlockX - blockX );
					blockDy = Math.abs( updateBlockY - blockY );
					blockDz = Math.abs( updateBlockZ - blockZ );
					boolean isRoomFor6Updates = updateCounter < m_lightUpdateBlockList.length - 6;
					if( blockDx + blockDy + blockDz < 17 && isRoomFor6Updates )
					{
						// queue updates for all 6 face-neighbors with 0
						// light
						if( world.getSavedLightValue( lightType, updateBlockX - 1, updateBlockY, updateBlockZ ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX - 1 - blockX + 32 + ( updateBlockY - blockY + 32 << 6 ) + ( updateBlockZ - blockZ + 32 << 12 );
						}
						
						if( world.getSavedLightValue( lightType, updateBlockX + 1, updateBlockY, updateBlockZ ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX + 1 - blockX + 32 + ( updateBlockY - blockY + 32 << 6 ) + ( updateBlockZ - blockZ + 32 << 12 );
						}
						
						if( world.getSavedLightValue( lightType, updateBlockX, updateBlockY - 1, updateBlockZ ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX - blockX + 32 + ( updateBlockY - 1 - blockY + 32 << 6 ) + ( updateBlockZ - blockZ + 32 << 12 );
						}
						
						if( world.getSavedLightValue( lightType, updateBlockX, updateBlockY + 1, updateBlockZ ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX - blockX + 32 + ( updateBlockY + 1 - blockY + 32 << 6 ) + ( updateBlockZ - blockZ + 32 << 12 );
						}
						
						if( world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ - 1 ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX - blockX + 32 + ( updateBlockY - blockY + 32 << 6 ) + ( updateBlockZ - 1 - blockZ + 32 << 12 );
						}
						
						if( world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ + 1 ) < computedLight )
						{
							m_lightUpdateBlockList[updateCounter++] = updateBlockX - blockX + 32 + ( updateBlockY - blockY + 32 << 6 ) + ( updateBlockZ + 1 - blockZ + 32 << 12 );
						}
					}
				}
			}
			
			// TEMP
			if( i > 10000 )
			{
				System.out.println( String.format( "%s Warning! Calculated %d light updates at (%d,%d,%d) for %s light.", world.isClient ? "CLIENT" : "SERVER", i, blockX, blockY, blockZ, lightType.name() ) );
			}
			
			world.theProfiler.endSection();
		}
		
		return true;
	}
	
	private int computeLightValue( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		if( lightType == EnumSkyBlock.Sky && world.canBlockSeeTheSky( blockX, blockY, blockZ ) )
		{
			// sky light is easy
			return 15;
		}
		else
		{
			Block block = world.getBlock( blockX, blockY, blockZ );
			
			// init this block's computed light with the light it generates
			int lightAtThisBlock = lightType == EnumSkyBlock.Sky ? 0 : block.getLightValue();
			
			int blockOpacity = block.getLightOpacity();
			
			// if the block emits light and also blocks it
			if( blockOpacity >= 15 && block.getLightValue() > 0 )
			{
				// reduce blocking
				blockOpacity = 1;
			}
			
			// min clamp on opacity
			if( blockOpacity < 1 )
			{
				blockOpacity = 1;
			}
			
			// if the block still blocks light (meaning, it couldn't have emitted light)
			// also, an opacity of this or higher means it could block all neighbor light
			if( blockOpacity >= 15 )
			{
				return 0;
			}
			// if the block already has the max light
			else if( lightAtThisBlock >= 14 )
			{
				return lightAtThisBlock;
			}
			else
			{
				// for each block face...
				for( int side = 0; side < 6; ++side )
				{
					int offsetBlockX = blockX + Facing.offsetsXForSide[side];
					int offsetBlockY = blockY + Facing.offsetsYForSide[side];
					int offsetBlockZ = blockZ + Facing.offsetsZForSide[side];
					
					int lightFromNeighbor = world.getSavedLightValue( lightType, offsetBlockX, offsetBlockY, offsetBlockZ ) - blockOpacity;
					
					// take the max of light from neighbors
					if( lightFromNeighbor > lightAtThisBlock )
					{
						lightAtThisBlock = lightFromNeighbor;
					}
					
					// short circuit to skip the rest of the neighbors
					if( lightAtThisBlock >= 14 )
					{
						return lightAtThisBlock;
					}
				}
				
				return lightAtThisBlock;
			}
		}
	}
}
