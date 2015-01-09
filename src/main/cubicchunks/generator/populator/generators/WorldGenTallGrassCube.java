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
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTallGrass;

public class WorldGenTallGrassCube extends WorldGeneratorCube
{

	private final Block block;
	private final int meta;

	public WorldGenTallGrassCube( Block block, int meta )
	{
		this.block = block;
		this.meta = meta;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		Block block_temp;

		int minY = getMinBlockYFromRandY( y );

		//Find top block (if not below minY)
		while( ((block_temp = world.getBlock( x, y, z )).getMaterial() == Material.air || block_temp.getMaterial() == Material.leaves) )
		{
			//Do not generate if we reach minY without finding any solid blocks.
			if( --y < minY )
			{
				return false;
			}
		}

		for( int i = 0; i < 128; ++i )
		{
			int xGen = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yGen = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zGen = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xGen, yGen, zGen ) && this.block.canBlockStay( world, xGen, yGen, zGen ) )
			{
				world.setBlock( xGen, yGen, zGen, this.block, this.meta, 2 );
			}
		}

		return true;
	}
}
