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
package cuchaz.cubicChunks;

import java.util.Collection;

public class CuboidalCubicChunkSelector extends CubicChunkSelector
{
	private static final int ViewDistanceY = 2;
	
	@Override
	protected void computeVisible( Collection<Long> out, int chunkX, int chunkY, int chunkZ, int viewDistance )
	{
		for( int x=chunkX-viewDistance; x<=chunkX+viewDistance; x++ )
		{
			for( int y=Math.max( 0, chunkY-ViewDistanceY ); y<=chunkY+ViewDistanceY; y++ )
			{
				for( int z=chunkZ-viewDistance; z<=chunkZ+viewDistance; z++ )
				{
					out.add( AddressTools.getAddress( x, y, z ) );
				}
			}
		}
	}
}
