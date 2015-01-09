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

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class WorldGenIceSpikeCube extends WorldGeneratorCube
{
	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int minY = getMinBlockYFromRandY( y );
		while( world.isAirBlock( x, y, z ) )
		{
			if( --y < minY )
			{
				return false;
			}
		}

		if( world.getBlock( x, y, z ) != Blocks.snow )
		{
			return false;
		}
		
		y += rand.nextInt( 4 );
		
		int maxHeight = rand.nextInt( 4 ) + 7;
		int maxRadius = maxHeight / 4 + rand.nextInt( 2 );

		if( maxRadius > 1 && rand.nextInt( 60 ) == 0 )
		{
			y += 10 + rand.nextInt( 30 );
		}

		for( int yRel = 0; yRel < maxHeight; ++yRel )
		{
			float radius = (1.0F - yRel / maxHeight) * maxRadius;
			int radius_int = MathHelper.ceiling_float_int( radius );

			for( int xRel = -radius_int; xRel <= radius_int; ++xRel )
			{
				float xRelMinus0_25 = MathHelper.abs_int( xRel ) - 0.25F;

				for( int zRel = -radius_int; zRel <= radius_int; ++zRel )
				{
					float zRelMinus0_25 = MathHelper.abs_int( zRel ) - 0.25F;

					if( (xRel == 0 && zRel == 0 || 
						xRelMinus0_25 * xRelMinus0_25 + zRelMinus0_25 * zRelMinus0_25 <= radius * radius) && 
						(xRel != -radius_int && xRel != radius_int && zRel != -radius_int && zRel != radius_int || 
						rand.nextFloat() <= 0.75F) )
					{
						Block block = world.getBlock( x + xRel, y + yRel, z + zRel );

						if( block.getMaterial() == Material.air || block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice )
						{
							this.setBlock(world, x + xRel, y + yRel, z + zRel, Blocks.packed_ice );
						}

						if( yRel != 0 && radius_int > 1 )
						{
							block = world.getBlock( x + xRel, y - yRel, z + zRel );

							if( block.getMaterial() == Material.air || block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice )
							{
								this.setBlock( world, x + xRel, y - yRel, z + zRel, Blocks.packed_ice );
							}
						}
					}
				}
			}
		}

		int maxRadiusMinusOne = maxRadius - 1;

		if( maxRadiusMinusOne < 0 )
		{
			maxRadiusMinusOne = 0;
		}
		else if( maxRadiusMinusOne > 1 )
		{
			maxRadiusMinusOne = 1;
		}

		for( int xRel = -maxRadiusMinusOne; xRel <= maxRadiusMinusOne; ++xRel )
		{
			int zRel = -maxRadiusMinusOne;

			while( zRel <= maxRadiusMinusOne )
			{
				int yAbs = y - 1;
				int i = 50;

				if( Math.abs( xRel ) == 1 && Math.abs( zRel ) == 1 )
				{
					i = rand.nextInt( 5 );
				}

				while( true )
				{
					if( yAbs > 50 )
					{
						Block block = world.getBlock( x + xRel, yAbs, z + zRel );

						if( block.getMaterial() == Material.air || 
							block == Blocks.dirt || 
							block == Blocks.snow || 
							block == Blocks.ice || 
							block == Blocks.packed_ice )
						{
							this.setBlock( world, x + xRel, yAbs, z + zRel, Blocks.packed_ice );
							--yAbs;
							--i;

							if( i <= 0 )
							{
								yAbs -= rand.nextInt( 5 ) + 1;
								i = rand.nextInt( 5 );
							}

							continue;
						}
					}

					++zRel;
					break;
				}
			}
		}

		return true;
	}
}
