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
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenDoublePlantCube extends WorldGeneratorCube
{
	private int metadata;

	public void setType( int metadata )
	{
		this.metadata = metadata;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		boolean generated = false;

		for( int i = 0; i < 64; ++i )
		{
			int xAbs = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yAbs = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zAbs = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xAbs, yAbs, zAbs ) && (!world.provider.hasNoSky) && Blocks.double_plant.canPlaceBlockAt( world, xAbs, yAbs, zAbs ) )
			{
				Blocks.double_plant.func_149889_c( world, xAbs, yAbs, zAbs, this.metadata, 2 );
				generated = true;
			}
		}

		return generated;
	}
}
