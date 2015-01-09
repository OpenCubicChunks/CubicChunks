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

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenSwampCube extends WorldGenAbstractTreeCube
{
	public WorldGenSwampCube()
	{
		super( false );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int minY = getMinBlockYFromRandY( y );

		while( world.getBlock( x, y - 1, z ).getMaterial() == Material.water )
		{
			if( --y < minY )
			{
				return false;
			}
		}

		int height = rand.nextInt( 4 ) + 5;

		boolean canGenerate = true;

		for( int yAbs = y; yAbs <= y + 1 + height; ++yAbs )
		{
			byte radius = 1;

			if( yAbs == y )
			{
				radius = 0;
			}

			if( yAbs >= y + 1 + height - 2 )
			{
				radius = 3;
			}

			for( int xAbs = x - radius; xAbs <= x + radius && canGenerate; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius && canGenerate; ++zAbs )
				{
					Block block = world.getBlock( xAbs, yAbs, zAbs );

					if( block.getMaterial() != Material.air && block.getMaterial() != Material.leaves )
					{
						if( block != Blocks.water && block != Blocks.flowing_water )
						{
							canGenerate = false;
						}
						else if( yAbs > y )
						{
							canGenerate = false;
						}
					}
				}
			}
		}

		if( !canGenerate )
		{
			return false;
		}

		Block blockBelow = world.getBlock( x, y - 1, z );

		if( blockBelow != Blocks.grass && blockBelow != Blocks.dirt )
		{
			return false;
		}

		this.setBlock( world, x, y - 1, z, Blocks.dirt );

		for( int yAbs = y - 3 + height; yAbs <= y + height; ++yAbs )
		{
			int yRel = yAbs - (y + height);
			int radius = 2 - yRel / 2;

			for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
			{
				int xRel = xAbs - x;

				for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
				{
					int zRel = zAbs - z;

					if( (Math.abs( xRel ) != radius
						|| Math.abs( zRel ) != radius
						|| rand.nextInt( 2 ) != 0 && yRel != 0)
						&& !world.getBlock( xAbs, yAbs, zAbs ).func_149730_j() )
					{
						this.setBlock( world, xAbs, yAbs, zAbs, Blocks.leaves );
					}
				}
			}
		}

		for( int yRel = 0; yRel < height; ++yRel )
		{
			Block block = world.getBlock( x, y + yRel, z );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves || block == Blocks.flowing_water || block == Blocks.water )
			{
				this.func_150515_a( world, x, y + yRel, z, Blocks.log );
			}
		}

		for( int yAbs = y - 3 + height; yAbs <= y + height; ++yAbs )
		{
			int yRel = yAbs - (y + height);
			int radius = 2 - yRel / 2;

			for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
				{
					if( world.getBlock( xAbs, yAbs, zAbs ).getMaterial() == Material.leaves )
					{
						if( rand.nextInt( 4 ) == 0 && world.getBlock( xAbs - 1, yAbs, zAbs ).getMaterial() == Material.air )
						{
							this.generateVines( world, xAbs - 1, yAbs, zAbs, 8 );
						}

						if( rand.nextInt( 4 ) == 0 && world.getBlock( xAbs + 1, yAbs, zAbs ).getMaterial() == Material.air )
						{
							this.generateVines( world, xAbs + 1, yAbs, zAbs, 2 );
						}

						if( rand.nextInt( 4 ) == 0 && world.getBlock( xAbs, yAbs, zAbs - 1 ).getMaterial() == Material.air )
						{
							this.generateVines( world, xAbs, yAbs, zAbs - 1, 1 );
						}

						if( rand.nextInt( 4 ) == 0 && world.getBlock( xAbs, yAbs, zAbs + 1 ).getMaterial() == Material.air )
						{
							this.generateVines( world, xAbs, yAbs, zAbs + 1, 4 );
						}
					}
				}
			}
		}

		return true;
	}

	/**
	 * Generates vines at the given position until it hits a block.
	 */
	private void generateVines( World world, int x, int y, int z, int rotation )
	{
		this.func_150516_a( world, x, y, z, Blocks.vine, rotation );
		int blocksLeft = 4;

		while( blocksLeft > 0 )
		{
			--blocksLeft;
			--y;

			if( world.getBlock( x, y, z ).getMaterial() != Material.air )
			{
				return;
			}

			this.setBlock( world, x, y, z, Blocks.vine, rotation );

		}
	}
}
