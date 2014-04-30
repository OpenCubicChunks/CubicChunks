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
package cuchaz.cubicChunks.visibility;

import java.util.Collection;

import cuchaz.cubicChunks.util.AddressTools;

public class EllipsoidalCubeSelector extends CubeSelector
{
	private static final int SemiAxisY = 3;
	private static final int SemiAxisY2 = SemiAxisY*SemiAxisY;
	
	@Override
	protected void computeVisible( Collection<Long> out, int cubeX, int cubeY, int cubeZ, int viewDistance )
	{
		// equation for an axis-aligned ellipsoid:
		// x^2/a^2 + y^2/b^2 + z^2/c^2 = 1
		// where a,b,c are the semi-principal axes
		int SemiAxisXZ = viewDistance;
		int SemiAxisXZ2 = SemiAxisXZ*SemiAxisXZ;
		
		for( int x=-SemiAxisXZ; x<=SemiAxisXZ; x++ )
		{
			int x2 = x*x;
			for( int z=-SemiAxisXZ; z<=SemiAxisXZ; z++ )
			{
				int z2 = z*z;
				int test = ( x2 + z2 - SemiAxisXZ2 )*SemiAxisY2;
				for( int y=-SemiAxisY; y<=SemiAxisY; y++ )
				{
					if( y + cubeY >= 0 )
					{
						int y2 = y*y;
						if( test <= -y2*SemiAxisXZ2 ) // test for point in ellipsoid, but using only integer arithmetic
						{
							out.add( AddressTools.getAddress( x + cubeX, y + cubeY, z + cubeZ ) );
						}
					}
				}
			}
		}
	}
}
