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

import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;

public class FirstLightCalculator implements ColumnCalculator
{
	@Override
	public boolean calculate( Column column )
	{
		boolean success = reallyCalculate( column );
		
		// set the lighting flag on the column so it gets properly sent to the client (or not)
		column.isLightPopulated = success;
		
		return success;
	}
	
	private boolean reallyCalculate( Column column )
	{
		// no sky? no sky light
		if( column.worldObj.provider.hasNoSky )
		{
			return true;
		}
		
		// only calculate first light if the neighboring columns exist
		// NOTE: doChunksNearChunkExist() essentially ignores the y coordinate
		int blockX = Coords.cubeToMinBlock( column.xPosition );
		int blockZ = Coords.cubeToMinBlock( column.zPosition );
		if( !column.worldObj.doChunksNearChunkExist( blockX, 0, blockZ, 17 ) )
		{
			return false;
		}
		
		for( int localX=0; localX<16; localX++ )
		{
			for( int localZ=0; localZ<16; localZ++ )
			{
				boolean wasLit = populateBlockColumnLighting( column, localX, localZ );
				
				// if the lighting failed, then try again later
				if( !wasLit )
				{
					return false;
				}
			}
		}
		
		/* try to populate neighboring columns too
		populateNeighborLight( -1, 0, 3 );
		populateNeighborLight( 1, 0, 1 );
		populateNeighborLight( 0, -1, 0 );
		populateNeighborLight( -1, 1, 2 );
		*/
		
		return true;
	}
	
	private boolean populateBlockColumnLighting( Column column, int localX, int localZ )
	{
		final int seaLevel = 63;
		
		int blockX = Coords.localToBlock( column.xPosition, localX );
		int blockZ = Coords.localToBlock( column.zPosition, localZ );
		
		boolean foundNonTransparentBlock = false;
		
		int blockY = column.getLightIndex().getTopNonTransparentBlock( localX, localZ ) + 1;
		for( ; blockY > 0; blockY-- )
		{
			int lightOpacity = column.func_150808_b( localX, blockY, localZ );
			
			if( lightOpacity == 255 && blockY < seaLevel )
			{
				// if we hit an opaque block below sea level, stop early
				break;
			}
			
			if( lightOpacity > 0 )
			{
				foundNonTransparentBlock = true;
			}
			
			// if we're below a non-transparent block and we're a clear block, then update lights for this block			
			if( foundNonTransparentBlock && lightOpacity == 0 )
			{
				boolean wasLit = column.worldObj.func_147451_t( blockX, blockY, blockZ );
				
				// if lighting failed, try again later
				if( !wasLit )
				{
					return false;
				}
			}
		}
		
		// update lights for light sources at this block and below
		for( ; blockY > 0; blockY-- )
		{
			if( column.func_150810_a( localX, blockY, localZ ).getLightValue() > 0 )
			{
				column.worldObj.func_147451_t( blockX, blockY, blockZ );
			}
		}
		
		return true;
	}
	
	private void populateNeighborLight( Column column, int dcubeX, int dcubeZ, int edge )
	{
		Column neighbor = (Column)column.worldObj.getChunkFromChunkCoords( column.xPosition + dcubeX, column.zPosition + dcubeZ );
		populateEdgeLighting( neighbor, edge );
	}
	
	private void populateEdgeLighting( Column column, int edge ) // 0,1,2, or 3
	{
		if( !column.isTerrainPopulated )
		{
			return;
		}
		
		if( edge == 3 )
		{
			for( int i=0; i<16; i++ )
			{
				populateBlockColumnLighting( column, 15, i );
			}
		}
		else if( edge == 1 )
		{
			for( int i=0; i<16; i++ )
			{
				populateBlockColumnLighting( column, 0, i );
			}
		}
		else if( edge == 0 )
		{
			for( int i=0; i<16; i++ )
			{
				populateBlockColumnLighting( column, i, 15 );
			}
		}
		else if( edge == 2 )
		{
			for( int i=0; i<16; i++ )
			{
				populateBlockColumnLighting( column, i, 0 );
			}
		}
	}
}
