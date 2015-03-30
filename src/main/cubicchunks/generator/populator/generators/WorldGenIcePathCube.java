/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.generator.populator.generators;

import cubicchunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class WorldGenIcePathCube extends WorldGeneratorCube {
	private final Block block;
	private int s;

	public WorldGenIcePathCube(int s) {
		this.block = Blocks.packed_ice;
		this.s = s;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int minY = getMinBlockYFromRandY(y);
		while (world.isAirBlock(x, y, z)) {
			if (--y < minY) {
				return false;
			}
		}

		if (world.getBlock(x, y, z) != Blocks.snow) {
			return false;
		}

		int radius = rand.nextInt(this.s - 2) + 2;
		byte yRadius = 1;

		for (int xAbs = x - radius; xAbs <= x + radius; ++xAbs) {
			for (int zAbs = z - radius; zAbs <= z + radius; ++zAbs) {
				int xDist = xAbs - x;
				int zDist = zAbs - z;

				if (xDist * xDist + zDist * zDist <= radius * radius) {
					for (int yAbs = y - yRadius; yAbs <= y + yRadius; ++yAbs) {
						Block block = world.getBlock(xAbs, yAbs, zAbs);

						if (block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice) {
							world.setBlock(xAbs, yAbs, zAbs, this.block, 0, 2);
						}
					}
				}
			}
		}

		return true;
	}
}
