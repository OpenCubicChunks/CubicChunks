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

import java.util.List;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.world.Cube;

public abstract class CubeCalculator implements LightCalculator
{
	@Override
	public int processBatch( List<Long> addresses, List<Long> deferredAddresses, CubeProvider provider )
	{
		// start processing
		int numSuccesses = 0;
		for( long address : addresses )
		{
			// get the cube
			int cubeX = AddressTools.getX( address );
			int cubeY = AddressTools.getY( address );
			int cubeZ = AddressTools.getZ( address );
			if( !provider.cubeExists( cubeX, cubeY, cubeZ ) )
			{
				continue;
			}
			Cube cube = provider.loadCube( cubeX, cubeY, cubeZ );
			
			// skip blank cubes
			if( cube.isEmpty() )
			{
				continue;
			}
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate( cube );
			if( !success )
			{
				deferredAddresses.add( address );
			}
			else
			{
				numSuccesses++;
			}
		}
		return numSuccesses;
	}
	
	public abstract boolean calculate( Cube cube );
}
