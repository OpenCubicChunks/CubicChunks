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
 *******************************************************************************/
package cuchaz.cubicChunks.generator.populator.generators;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenShrubCube extends WorldGenTreesCube
{
	private static final int HEIGHT = 2;
	private final int leavesMetadata;
	private final int logMetadata;

	public WorldGenShrubCube( int par1, int par2 )
	{
		super( false );
		this.logMetadata = par1;
		this.leavesMetadata = par2;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		Block block;

		int minY = getMinBlockYFromRandY( y );
		while( ((block = world.getBlock( x, y, z )).getMaterial() == Material.air || block.getMaterial() == Material.leaves) )
		{
			if( --y < minY )
			{
				return false;
			}
		}

		block = world.getBlock( x, y, z );

		if( block == Blocks.dirt || block == Blocks.grass )
		{
			++y;
			this.setBlock( world, x, y, z, Blocks.log, this.logMetadata );

			for( int yAbs = y; yAbs <= y + HEIGHT; ++yAbs )
			{
				int yDist = yAbs - y;
				int radius = HEIGHT - yDist;

				for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
				{
					int xDist = xAbs - x;

					for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
					{
						int zDist = zAbs - z;

						if( (Math.abs( xDist ) != radius
							|| Math.abs( zDist ) != radius
							|| rand.nextInt( 2 ) != 0)
							&& !world.getBlock( xAbs, yAbs, zAbs ).func_149730_j() )
						{
							this.setBlock( world, xAbs, yAbs, zAbs, Blocks.leaves, this.leavesMetadata );
						}
					}
				}
			}
		}

		return true;
	}
}
