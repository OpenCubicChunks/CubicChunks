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
package main.java.cubicchunks.visibility;

import java.util.Collection;

import main.java.cubicchunks.util.AddressTools;

public class CuboidalCubeSelector extends CubeSelector
{
	private static final int ViewDistanceY = 8;
	
	@Override
	protected void computeVisible( Collection<Long> out, int cubeX, int cubeY, int cubeZ, int viewDistance )
	{
		for( int x=cubeX-viewDistance; x<=cubeX+viewDistance; x++ )
		{
			for( int y=cubeY-ViewDistanceY; y<=cubeY+ViewDistanceY; y++ )
			{
				for( int z=cubeZ-viewDistance; z<=cubeZ+viewDistance; z++ )
				{
					out.add( AddressTools.getAddress( x, y, z ) );
				}
			}
		}
	}
}
