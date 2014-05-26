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
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.util.CubeCoordinate;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import cuchaz.cubicChunks.world.LightIndex;

public class FirstLightProcessor extends CubeProcessor
{
	public FirstLightProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		// only light if the neighboring cubes exist
		CubeProvider provider = ((CubeWorld)cube.getWorld()).getCubeProvider();
		if( !CubeProviderTools.cubeAndNeighborsExist( provider, cube.getX(), cube.getY(), cube.getZ() ) )
		{
			return false;
		}
		
		// update the sky light
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				updateSkylight( cube, localX, localZ );
			}
		}
		
		// light blocks in this cube
		for( int localX=0; localX<16; localX++ )
		{
			for( int localY=0; localY<16; localY++ )
			{
				for( int localZ=0; localZ<16; localZ++ )
				{
					boolean wasLit = lightBlock( cube, localX, localY, localZ );
					
					// if the lighting failed, then try again later
					if( !wasLit )
					{
						return false;
					}
				}
			}
		}
		
		// populate the nearby faces of adjacent cubes
		// this is for cases when a sheer wall is up against an empty cube
		// unless this is called, the wall will not get directly lit
		lightXSlab( provider.provideCube( cube.getX() - 1, cube.getY(), cube.getZ() ), 15 );
		lightXSlab( provider.provideCube( cube.getX() + 1, cube.getY(), cube.getZ() ), 0 );
		lightYSlab( provider.provideCube( cube.getX(), cube.getY() - 1, cube.getZ() ), 15 );
		lightYSlab( provider.provideCube( cube.getX(), cube.getY() + 1, cube.getZ() ), 0 );
		lightZSlab( provider.provideCube( cube.getX(), cube.getY(), cube.getZ() - 1 ), 15 );
		lightZSlab( provider.provideCube( cube.getX(), cube.getY(), cube.getZ() + 1 ), 0 );
		
		return true;
	}
	
	private void updateSkylight( Cube cube, int localX, int localZ )
	{
		// compute bounds on the sky light gradient
		LightIndex index = cube.getColumn().getLightIndex();
		int lightMaxBlockY = index.getTopNonTransparentBlock( localX, localZ ) + 1; // transparent block on top of non-transparent block
		int lightMinBlockY = lightMaxBlockY - 15;
		
		// get the cube bounds
		int cubeMinBlockY = CubeCoordinate.cubeToMinBlock( cube.getY() );
		int cubeMaxBlockY = CubeCoordinate.cubeToMaxBlock( cube.getY() );
		
		// could this sky light possibly reach this cube?
		if( cubeMinBlockY > lightMaxBlockY )
		{
			// set everything to sky light
			for( int localY=0; localY<16; localY++ )
			{
				cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, 15 );
			}
		}
		else if( cubeMaxBlockY < lightMinBlockY )
		{
			// set everything to dark
			for( int localY=0; localY<16; localY++ )
			{
				cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, 0 );
			}
		}
		else
		{
			// need to calculate the light
			int light = 15;
			int startBlockY = Math.max( lightMaxBlockY, cubeMaxBlockY );
			for( int blockY=startBlockY; blockY>=cubeMinBlockY; blockY-- )
			{
				int opacity = index.getOpacity( localX, blockY, localZ );
				if( opacity == 0 && light < 15 )
				{
					// after something blocks light, apply a linear falloff
					opacity = 1;
				}
				
				// decrease the light
				light = Math.max( 0, light - opacity );
				
				if( blockY <= cubeMaxBlockY )
				{
					// apply the light
					int localY = CubeCoordinate.blockToLocal( blockY );
					cube.setLightValue( EnumSkyBlock.Sky, localX, localY, localZ, light );
				}
			}
		}
	}

	private void lightXSlab( Cube cube, int localX )
	{
		for( int localY=0; localY<16; localY++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				lightBlock( cube, localX, localY, localZ );
			}
		}
	}
	
	private void lightYSlab( Cube cube, int localY )
	{
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				lightBlock( cube, localX, localY, localZ );
			}
		}
	}
	
	private void lightZSlab( Cube cube, int localZ )
	{
		for( int localX=0; localX<16; localX++ )
		{
			for( int localY=0; localY<16; localY++ )
			{
				lightBlock( cube, localX, localY, localZ );
			}
		}
	}
	
	private boolean lightBlock( Cube cube, int localX, int localY, int localZ )
	{
		// conditions for lighting a block in phase 1:
		//    must be below a non-transparent block
		//    must be above an opaque block that's below sea level
		//    must be a clear block
		//    must have a sky
		
		// conditions for lighting a block in phase 2:
		//   must be at or below an opaque block below sea level
		//   must be a block light source
		
		int blockY = CubeCoordinate.localToBlock( cube.getY(), localY );
		LightIndex index = cube.getColumn().getLightIndex();
		
		boolean lightBlock = false;
		if( blockY > index.getTopOpaqueBlockBelowSeaLevel( localX, localZ ) )
		{
			if( !cube.getColumn().worldObj.provider.hasNoSky && blockY < index.getTopNonTransparentBlock( localX, localZ ) && index.getOpacity( localX, blockY, localZ ) == 0 )
			{
				lightBlock = true;
			}
		}
		else if( cube.getBlock( localX, localY, localZ ).getLightValue() > 0 )
		{
			lightBlock = true;
		}
		
		if( lightBlock )
		{
			int blockX = CubeCoordinate.localToBlock( cube.getX(), localX );
			int blockZ = CubeCoordinate.localToBlock( cube.getZ(), localZ );
			return cube.getWorld().func_147451_t( blockX, blockY, blockZ );
		}
		
		return true;
	}
}
