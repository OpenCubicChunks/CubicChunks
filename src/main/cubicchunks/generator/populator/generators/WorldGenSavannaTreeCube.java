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
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class WorldGenSavannaTreeCube extends WorldGenAbstractTreeCube
{
	public WorldGenSavannaTreeCube( boolean flag )
	{
		super( flag );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int treeHeight = rand.nextInt( 3 ) + rand.nextInt( 3 ) + 5;
		boolean canGenerate = true;

		for( int yAbs = y; yAbs <= y + 1 + treeHeight; ++yAbs )
		{
			byte radius = 1;

			if( yAbs == y )
			{
				radius = 0;
			}

			if( yAbs >= y + 1 + treeHeight - 2 )
			{
				radius = 2;
			}

			for( int xAbs = x - radius; xAbs <= x + radius && canGenerate; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius && canGenerate; ++zAbs )
				{
					Block block = world.getBlock( xAbs, yAbs, zAbs );

					if( !this.isReplacableFromTreeGen(block ) )
					{
						canGenerate = false;
					}
				}
			}
		}

		if( !canGenerate )
		{
			return false;
		}
		Block block = world.getBlock( x, y - 1, z );

		if( block != Blocks.grass && block != Blocks.dirt )
		{
			return false;
		}

		this.setBlock( world, x, y - 1, z, Blocks.dirt );
		int direction = rand.nextInt( 4 );
		int heightStart = treeHeight - rand.nextInt( 4 ) - 1;
		int heightLeft = 3 - rand.nextInt( 3 );
		int xAbs = x;
		int zAbs = z;
		int lastSetY = 0;

		for( int yRel = 0; yRel < treeHeight; ++yRel )
		{
			int yAbs = y + yRel;

			if( yRel >= heightStart && heightLeft > 0 )
			{
				xAbs += Direction.offsetX[direction];
				zAbs += Direction.offsetZ[direction];
				--heightLeft;
			}

			Block currentBlock = world.getBlock( xAbs, yAbs, zAbs );

			if( currentBlock.getMaterial() == Material.air || currentBlock.getMaterial() == Material.leaves )
			{
				this.setBlock( world, xAbs, yAbs, zAbs, Blocks.log2, 0 );
				lastSetY = yAbs;
			}
		}

		for( int xRel = -1; xRel <= 1; ++xRel )
		{
			for( int zRel = -1; zRel <= 1; ++zRel )
			{
				this.setLeavesIfCanReplace( world, xAbs + xRel, lastSetY + 1, zAbs + zRel );
			}
		}

		this.setLeavesIfCanReplace( world, xAbs + 2, lastSetY + 1, zAbs );
		this.setLeavesIfCanReplace( world, xAbs - 2, lastSetY + 1, zAbs );
		this.setLeavesIfCanReplace( world, xAbs, lastSetY + 1, zAbs + 2 );
		this.setLeavesIfCanReplace( world, xAbs, lastSetY + 1, zAbs - 2 );

		for( int xRel = -3; xRel <= 3; ++xRel )
		{
			for( int zRel = -3; zRel <= 3; ++zRel )
			{
				if( Math.abs( xRel ) != 3 || Math.abs( zRel ) != 3 )
				{
					this.setLeavesIfCanReplace( world, xAbs + xRel, lastSetY, zAbs + zRel );
				}
			}
		}

		xAbs = x;
		zAbs = z;
		int direction2 = rand.nextInt( 4 );

		if( direction2 != direction )
		{
			int yRelStart = heightStart - rand.nextInt( 2 ) - 1;
			int maxIterations = 1 + rand.nextInt( 3 );
			lastSetY = Integer.MIN_VALUE;
			int yAbs;
			int yRel;

			for( yRel = yRelStart; yRel < treeHeight && maxIterations > 0; --maxIterations )
			{
				if( yRel >= 1 )
				{
					yAbs = y + yRel;
					xAbs += Direction.offsetX[direction2];
					zAbs += Direction.offsetZ[direction2];
					Block currentBlock = world.getBlock( xAbs, yAbs, zAbs );

					if( currentBlock.getMaterial() == Material.air || currentBlock.getMaterial() == Material.leaves )
					{
						this.setBlock( world, xAbs, yAbs, zAbs, Blocks.log2, 0 );
						lastSetY = yAbs;
					}
				}

				++yRel;
			}

			if( lastSetY != Integer.MIN_VALUE )
			{
				for( int xRel = -1; xRel <= 1; ++xRel )
				{
					for( int zRel = -1; zRel <= 1; ++zRel )
					{
						this.setLeavesIfCanReplace( world, xAbs + xRel, lastSetY + 1, zAbs + zRel );
					}
				}

				for( int xRel = -2; xRel <= 2; ++xRel )
				{
					for( int zRel = -2; zRel <= 2; ++zRel )
					{
						if( Math.abs( xRel ) != 2 || Math.abs( zRel ) != 2 )
						{
							this.setLeavesIfCanReplace( world, xAbs + xRel, lastSetY, zAbs + zRel );
						}
					}
				}
			}
		}

		return true;
	}

	protected void setBlock( World world, int x, int y, int z, Block block )
	{
		this.func_150515_a( world, x, y, z, block );
	}

	protected void setBlock( World world, int x, int y, int z, Block block, int meta )
	{
		this.func_150516_a( world, x, y, z, block, meta );
	}

	private void setLeavesIfCanReplace( World world, int x, int y, int z )
	{
		Block block = world.getBlock( x, y, z );

		if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
		{
			this.setBlock( world, x, y, z, Blocks.leaves2, 0 );
		}
	}
}
