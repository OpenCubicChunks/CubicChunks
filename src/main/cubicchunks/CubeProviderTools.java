/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks;

import cubicchunks.util.Coords;

public class CubeProviderTools
{
	public static boolean blocksExist( CubeProvider provider, int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ )
	{
		// convert block bounds to chunk bounds
		int minCubeX = Coords.blockToCube( minBlockX );
		int minCubeY = Coords.blockToCube( minBlockY );
		int minCubeZ = Coords.blockToCube( minBlockZ );
		int maxCubeX = Coords.blockToCube( maxBlockX );
		int maxCubeY = Coords.blockToCube( maxBlockY );
		int maxCubeZ = Coords.blockToCube( maxBlockZ );
		
		return cubesExist( provider, minCubeX, minCubeY, minCubeZ, maxCubeX, maxCubeY, maxCubeZ );
	}
	
	public static boolean cubeAndNeighborsExist( CubeProvider provider, int cubeX, int cubeY, int cubeZ )
	{
		return cubesExist( provider,
			cubeX - 1, cubeY - 1, cubeZ - 1,
			cubeX + 1, cubeY + 1, cubeZ + 1
		);
	}
	
	public static boolean cubesExist( CubeProvider provider, int minCubeX, int minCubeY, int minCubeZ, int maxCubeX, int maxCubeY, int maxCubeZ )
	{
		for( int cubeX=minCubeX; cubeX<=maxCubeX; cubeX++ )
		{
			for( int cubeY=minCubeY; cubeY<=maxCubeY; cubeY++ )
			{
				for( int cubeZ=minCubeZ; cubeZ<=maxCubeZ; cubeZ++ )
				{
					if( !provider.cubeExists( cubeX, cubeY, cubeZ ) )
					{
						return false;
					}
				}
			}
		}
		return true;
	}
}
