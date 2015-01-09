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
package cuchaz.cubicChunks.generator.populator;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public abstract class WorldGeneratorCube extends WorldGenerator
{
	protected static int getMinBlockYFromRandY( int y )
	{
		//we get:
		//y = multiplyOf16 + 8 + randomValue

		//we want to get:
		//minY = multiplyOf16 + 8
		//
		//RandomValue is 0-15
		//
		//Substract 8. After this minY = multiplyOf16 + randomValue
		//int minY = y - 8;
		//
		//Remove randomValue (4 bits). After this minY = multiplyOf16
		//minY &= 0xFFFFFFF0;
		//
		//add 8 to get minY
		//minY += 8;
		
		int minY = ((y - 8) & 0xFFFFFFF0) + 8;
		assert y - minY >= 0 && y - minY < 16;
		return minY;
	}

	public WorldGeneratorCube()
	{
		super();
	}

	public WorldGeneratorCube( boolean doBlockNotify )
	{
		super( doBlockNotify );
	}

	//deobfuscated method names
	protected void setBlock( World world, int x, int y, int z, Block block )
	{
		this.func_150515_a( world, x, y, z, block );
	}

	protected void setBlock( World world, int x, int y, int z, Block block, int metadata )
	{
		this.func_150516_a( world, x, y, z, block, metadata );
	}
}
