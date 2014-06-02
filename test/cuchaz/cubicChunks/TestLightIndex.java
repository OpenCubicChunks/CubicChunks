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

import static org.junit.Assert.*;

import org.junit.Test;

import cuchaz.cubicChunks.world.LightIndex;

public class TestLightIndex
{
	private static final int SeaLevel = 0;
	
	@Test
	public void readWrite( )
	{
		LightIndex original = new LightIndex( SeaLevel);
		
		// make some changes
		original.setOpacity( 4, 10, 5, 10 );
		original.setOpacity( 6, 34, 10, 255 );
		
		// clone the index
		LightIndex copy = new LightIndex( SeaLevel );
		copy.readData( original.getData() );
		
		// make sure they're identical
		for( int x=0; x<16; x++ )
		{
			for( int z=0; z<16; z++ )
			{
				for( int y=0; y<255; y++ )
				{
					assertEquals( original.getOpacity( x, y, z ), copy.getOpacity( x, y, z ) );
				}
			}
		}
	}
}
