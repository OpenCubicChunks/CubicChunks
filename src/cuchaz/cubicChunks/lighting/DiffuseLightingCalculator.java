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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.util.Bits;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.FastIntQueue;

public class DiffuseLightingCalculator
{
	private static final Logger log = LogManager.getLogger();
	
	private FastIntQueue m_queue;
	
	public DiffuseLightingCalculator( )
	{
		m_queue = new FastIntQueue();
	}
	
	public boolean calculate( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		// are there enough nearby blocks to do the lighting?
		if( !world.doChunksNearChunkExist( blockX, blockY, blockZ, 16 ) )
		{
			return false;
		}
		
		m_queue.clear();
		
		// did we add or subtract light?
		int oldLight = world.getSavedLightValue( lightType, blockX, blockY, blockZ );
		int newLight = computeLightValue( world, blockX, blockY, blockZ, lightType );
		if( newLight > oldLight )
		{
			// seed processing with this block
			m_queue.add( packUpdate( 0, 0, 0, 0 ) );
		}
		else if( newLight < oldLight )
		{
			// subtract light from the area
			world.theProfiler.startSection( "diffuse light subtractions" );
			m_queue.add( packUpdate( 0, 0, 0, oldLight ) );
			processLightSubtractions( world, blockX, blockY, blockZ, lightType );
			world.theProfiler.endSection();
			
			// reset the queue so the next processing method re-processes all the entries
			m_queue.reset();
		}
		
		// add light to the area
		world.theProfiler.startSection( "diffuse light additions" );
		processLightAdditions( world, blockX, blockY, blockZ, lightType );
		world.theProfiler.endSection();
		
		// TEMP
		if( m_queue.size() > 32000 )
		{
			log.warn( String.format( "%s Warning! Calculated %d light updates at (%d,%d,%d) for %s light.",
				world.isClient ? "CLIENT" : "SERVER",
				m_queue.size(),
				blockX, blockY, blockZ,
				lightType.name()
			) );
		}
		
		return true;
	}
	
	private void processLightSubtractions( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		// for each queued light update...
		while( m_queue.hasNext() )
		{
			// unpack the update
			int update = m_queue.get();
			int updateBlockX = unpackUpdateDx( update ) + blockX;
			int updateBlockY = unpackUpdateDy( update ) + blockY;
			int updateBlockZ = unpackUpdateDz( update ) + blockZ;
			int updateLight = unpackUpdateLight( update );
			
			// if the light changed, skip this update
			int oldLight = world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ );
			if( oldLight != updateLight )
			{
				continue;
			}
			
			// set update block light to 0
			world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, 0 );
			
			// if we ran out of light, don't propagate
			if( updateLight <= 0 )
			{
				continue;
			}
			
			// for each neighbor block...
			for( int side=0; side<6; side++ )
			{
				// get the neighboring block coords
				int neighborBlockX = updateBlockX + Facing.offsetsXForSide[side];
				int neighborBlockY = updateBlockY + Facing.offsetsYForSide[side];
				int neighborBlockZ = updateBlockZ + Facing.offsetsZForSide[side];
				
				if( !shouldUpdateLight( world, blockX, blockY, blockZ, neighborBlockX, neighborBlockY, neighborBlockZ ) )
				{
					continue;
				}
				
				// get the neighbor opacity
				int neighborOpacity = world.getBlock( neighborBlockX, neighborBlockY, neighborBlockZ ).getLightOpacity();
				if( neighborOpacity < 1 )
				{
					neighborOpacity = 1;
				}
				
				// if the neighbor block doesn't have the light we expect, bail
				int expectedLight = updateLight - neighborOpacity;
				int actualLight = world.getSavedLightValue( lightType, neighborBlockX, neighborBlockY, neighborBlockZ );
				if( actualLight != expectedLight )
				{
					continue;
				}
				
				if( m_queue.hasRoomFor( 1 ) )
				{
					// queue an update to subtract light from the neighboring block
					m_queue.add( packUpdate(
						neighborBlockX - blockX,
						neighborBlockY - blockY,
						neighborBlockZ - blockZ,
						expectedLight
					) );
				}
			}
		}
	}
	
	private void processLightAdditions( World world, int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		// for each queued light update...
		while( m_queue.hasNext() )
		{
			// unpack the update
			int update = m_queue.get();
			int updateBlockX = unpackUpdateDx( update ) + blockX;
			int updateBlockY = unpackUpdateDy( update ) + blockY;
			int updateBlockZ = unpackUpdateDz( update ) + blockZ;
			
			// skip updates that don't change the light
			int oldLight = world.getSavedLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ );
			int newLight = computeLightValue( world, updateBlockX, updateBlockY, updateBlockZ, lightType );
			if( newLight == oldLight )
			{
				continue;
			}
			
			// update the light here
			world.setLightValue( lightType, updateBlockX, updateBlockY, updateBlockZ, newLight );
			
			// if we didn't get brighter, don't propagate light to the area
			if( newLight <= oldLight )
			{
				continue;
			}
			
			// for each neighbor block...
			for( int side=0; side<6; side++ )
			{
				// get the neighboring block coords
				int neighborBlockX = updateBlockX + Facing.offsetsXForSide[side];
				int neighborBlockY = updateBlockY + Facing.offsetsYForSide[side];
				int neighborBlockZ = updateBlockZ + Facing.offsetsZForSide[side];
				
				if( !shouldUpdateLight( world, blockX, blockY, blockZ, neighborBlockX, neighborBlockY, neighborBlockZ ) )
				{
					continue;
				}
				
				// if the neighbor already has enough light, bail
				int neighborLight = world.getSavedLightValue( lightType, neighborBlockX, neighborBlockY, neighborBlockZ );
				if( neighborLight >= newLight )
				{
					continue;
				}
				
				if( m_queue.hasRoomFor( 1 ) )
				{
					// queue an update to add light to the neighboring block
					m_queue.add( packUpdate(
						neighborBlockX - blockX,
						neighborBlockY - blockY,
						neighborBlockZ - blockZ,
						0
					) );
				}
			}
		}
	}
	
	private boolean shouldUpdateLight( World world, int blockX, int blockY, int blockZ, int targetBlockX, int targetBlockY, int targetBlockZ )
	{
		// don't update blocks that are too far away
		int manhattanDistance = MathHelper.abs_int( targetBlockX - blockX )
			+ MathHelper.abs_int( targetBlockY - blockY )
			+ MathHelper.abs_int( targetBlockZ - blockZ );
		if( manhattanDistance > 16 )
		{
			return false;
		}
		
		// don't update blocks can't write to
		if( !isLightModifiable( world, targetBlockX, targetBlockY, targetBlockZ ) )
		{
			return false;
		}
		
		return true;
	}
	
	private boolean isLightModifiable( World world, int blockX, int blockY, int blockZ )
	{
		int cubeX = Coords.blockToCube( blockX );
		int cubeY = Coords.blockToCube( blockY );
		int cubeZ = Coords.blockToCube( blockZ );
		
		CubeWorld cubeWorld = (CubeWorld)world;
		return cubeWorld.getCubeProvider().cubeExists( cubeX, cubeY, cubeZ );
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
	
	private int packUpdate( int dx, int dy, int dz, int light )
	{
		return Bits.packSignedToInt( dx, 6, 0 )
			| Bits.packSignedToInt( dy, 6, 6 )
			| Bits.packSignedToInt( dz, 6, 12 )
			| Bits.packUnsignedToInt( light, 6, 18 );
	}
	
	private int unpackUpdateDx( int packed )
	{
		return Bits.unpackSigned( packed, 6, 0 );
	}
	
	private int unpackUpdateDy( int packed )
	{
		return Bits.unpackSigned( packed, 6, 6 );
	}
	
	private int unpackUpdateDz( int packed )
	{
		return Bits.unpackSigned( packed, 6, 12 );
	}
	
	private int unpackUpdateLight( int packed )
	{
		return Bits.unpackUnsigned( packed, 6, 18 );
	}
}
