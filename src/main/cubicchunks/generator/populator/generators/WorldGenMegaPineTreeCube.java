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

import cuchaz.cubicChunks.generator.populator.WorldGenHugeTreesCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class WorldGenMegaPineTreeCube extends WorldGenHugeTreesCube
{
	private final boolean useBaseHeight;

	public WorldGenMegaPineTreeCube( boolean flag, boolean flag2 )
	{
		super( flag, 13, 15, 1, 1 );
		this.useBaseHeight = flag2;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int height = this.getRandomHeight( rand );

		if( !this.canGenerate( world, rand, x, y, z, height ) )
		{
			return false;
		}
		
		this.generateTreeBase( world, x, z, y + height, 0, rand );

		//Not <=. Don't set block at y + height, top blocks always leaves blocks
		for( int yRel = 0; yRel < height; ++yRel )
		{
			Block block = world.getBlock( x, y + yRel, z );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
			{
				this.setBlock( world, x, y + yRel, z, Blocks.log, this.woodMetadata );
			}

			//...???
			if( yRel < height - 1 )
			{
				//2x2 blocks tree
				block = world.getBlock( x + 1, y + yRel, z );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x + 1, y + yRel, z, Blocks.log, this.woodMetadata );
				}

				block = world.getBlock( x + 1, y + yRel, z + 1 );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x + 1, y + yRel, z + 1, Blocks.log, this.woodMetadata );
				}

				block = world.getBlock( x, y + yRel, z + 1 );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x, y + yRel, z + 1, Blocks.log, this.woodMetadata );
				}
			}
		}

		return true;
	}

	//???
	@Override
	public void afterGenerate( World world, Random rand, int x, int y, int z)
	{
		this.createDirtBlocksBelowTree( world, rand, x - 1, y, z - 1 );
		this.createDirtBlocksBelowTree( world, rand, x + 2, y, z - 1 );
		this.createDirtBlocksBelowTree( world, rand, x - 1, y, z + 2 );
		this.createDirtBlocksBelowTree( world, rand, x + 2, y, z + 2 );
		
		for( int i = 0; i < 5; ++i )
		{
			int randint = rand.nextInt( 64 );
			int randint1 = randint % 8;
			int randint2 = randint / 8;
			
			if( randint1 == 0 || randint1 == 7 || randint2 == 0 || randint2 == 7 )
			{
				this.createDirtBlocksBelowTree( world, rand, x - 3 + randint1, y, z - 3 + randint2 );
			}
		}
	}

	private void generateTreeBase( World world, int x, int z, int maxY, int radiusAdd, Random rand)
	{
		int leavesHeight = rand.nextInt( 5 );
		
		if( this.useBaseHeight )
		{
			leavesHeight += this.baseHeight;
		}
		else
		{
			leavesHeight += 3;
		}

		int lastRadiusBase = 0;

		for( int yAbs = maxY - leavesHeight; yAbs <= maxY; ++yAbs )
		{
			int yRel = maxY - yAbs;
			int radiusBase = radiusAdd + MathHelper.floor_float( (float)yRel / (float)leavesHeight * 3.5F );
			//add 1 to radius if it's not max height AND last iteration radiusBase was the same AND yAbs is even number...
			//is it because it's 2x2 tree?
			int radius = radiusBase + (yRel > 0 && radiusBase == lastRadiusBase && (yAbs & 1) == 0 ? 1 : 0);
			this.generateLayerAtLocationWithRadius2x2(world, x, yAbs, z, radius, rand );
			lastRadiusBase = radiusBase;
		}
	}

	//It seems to create dirt blocks for tree, but I'm not sure
	private void createDirtBlocksBelowTree( World world, Random rand, int x, int y, int z )
	{
		for( int xRel = -2; xRel <= 2; ++xRel )
		{
			for( int zRel = -2; zRel <= 2; ++zRel )
			{
				if( Math.abs( xRel ) != 2 || Math.abs( zRel ) != 2 )
				{
					this.setDirtBlockForTree( world, x + xRel, y, z + zRel );
				}
			}
		}
	}

	//it replaces one ditr/grass block with dirt.
	private void setDirtBlockForTree( World world, int x, int y, int z )
	{
		for( int yAbs = y + 2; yAbs >= y - 3; --yAbs )
		{
			Block block = world.getBlock( x, yAbs, z );

			if( block == Blocks.grass || block == Blocks.dirt )
			{
				this.setBlock( world, x, yAbs, z, Blocks.dirt, 2 );
				break;
			}

			if( block.getMaterial() != Material.air && yAbs < y )
			{
				break;
			}
		}
	}
}
