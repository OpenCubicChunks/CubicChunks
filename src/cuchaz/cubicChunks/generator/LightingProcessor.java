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
package cuchaz.cubicChunks.generator;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import cuchaz.cubicChunks.world.LightIndex;

public class LightingProcessor extends CubeProcessor
{
	public LightingProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		// already lit? we're done here
		if( cube.isLit() )
		{
			return true;
		}
		
		boolean success = reallyCalculate( cube );
		
		// set the lighting flag on the cube so it gets properly sent to the client (or not)
		cube.setIsLit( success );
		
		return success;
	}
	
	private boolean reallyCalculate( Cube cube )
	{
		// only light if the neighboring cubes exist
		CubeProvider provider = ((CubeWorld)cube.getWorld()).getCubeProvider();
		if( !CubeProviderTools.cubesExist( provider, cube.getX()-1, cube.getY()-1, cube.getZ()-1, cube.getX()+1, cube.getY()+1, cube.getZ()+1 ) )
		{
			return false;
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
		lightXSlab( provider.loadCube( cube.getX() - 1, cube.getY(), cube.getZ() ), 15 );
		lightXSlab( provider.loadCube( cube.getX() + 1, cube.getY(), cube.getZ() ), 0 );
		lightYSlab( provider.loadCube( cube.getX(), cube.getY() - 1, cube.getZ() ), 15 );
		lightYSlab( provider.loadCube( cube.getX(), cube.getY() + 1, cube.getZ() ), 0 );
		lightZSlab( provider.loadCube( cube.getX(), cube.getY(), cube.getZ() - 1 ), 15 );
		lightZSlab( provider.loadCube( cube.getX(), cube.getY(), cube.getZ() + 1 ), 0 );
		
		return true;
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
		
		int blockY = Coords.localToBlock( cube.getY(), localY );
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
			int blockX = Coords.localToBlock( cube.getX(), localX );
			int blockZ = Coords.localToBlock( cube.getZ(), localZ );
			return cube.getWorld().func_147451_t( blockX, blockY, blockZ );
		}
		
		return true;
	}
}
