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
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class WorldGenDesertWellsCube extends WorldGeneratorCube {
	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int minY = getMinBlockYFromRandY(y);
		y++;

		while (world.isAirBlock(x, y, z)) {
			if (--y < minY) {
				return false;
			}
		}

		if (world.getBlock(x, y, z) != Blocks.sand) {
			return false;
		}

		for (int xRel = -2; xRel <= 2; ++xRel) {
			for (int zRel = -2; zRel <= 2; ++zRel) {
				if (world.isAirBlock(x + xRel, y - 1, z + zRel) && world.isAirBlock(x + xRel, y - 2, z + zRel)) {
					return false;
				}
			}
		}

		for (int xRel = -1; xRel <= 0; ++xRel) {
			for (int yRel = -2; yRel <= 2; ++yRel) {
				for (int zRel = -2; zRel <= 2; ++zRel) {
					world.setBlock(x + yRel, y + yRel, z + zRel, Blocks.sandstone, 0, 2);
				}
			}
		}

		world.setBlock(x, y, z, Blocks.flowing_water, 0, 2);
		world.setBlock(x - 1, y, z, Blocks.flowing_water, 0, 2);
		world.setBlock(x + 1, y, z, Blocks.flowing_water, 0, 2);
		world.setBlock(x, y, z - 1, Blocks.flowing_water, 0, 2);
		world.setBlock(x, y, z + 1, Blocks.flowing_water, 0, 2);

		for (int xRel = -2; xRel <= 2; ++xRel) {
			for (int zRel = -2; zRel <= 2; ++zRel) {
				if (xRel == -2 || xRel == 2 || zRel == -2 || zRel == 2) {
					world.setBlock(x + xRel, y + 1, z + zRel, Blocks.sandstone, 0, 2);
				}
			}
		}

		world.setBlock(x + 2, y + 1, z, Blocks.stone_slab, 1, 2);
		world.setBlock(x - 2, y + 1, z, Blocks.stone_slab, 1, 2);
		world.setBlock(x, y + 1, z + 2, Blocks.stone_slab, 1, 2);
		world.setBlock(x, y + 1, z - 2, Blocks.stone_slab, 1, 2);

		for (int xRel = -1; xRel <= 1; ++xRel) {
			for (int zRel = -1; zRel <= 1; ++zRel) {
				if (xRel == 0 && zRel == 0) {
					world.setBlock(x + xRel, y + 4, z + zRel, Blocks.sandstone, 0, 2);
				} else {
					world.setBlock(x + xRel, y + 4, z + zRel, Blocks.stone_slab, 1, 2);
				}
			}
		}

		for (int yRel = 1; yRel <= 3; ++yRel) {
			world.setBlock(x - 1, y + yRel, z - 1, Blocks.sandstone, 0, 2);
			world.setBlock(x - 1, y + yRel, z + 1, Blocks.sandstone, 0, 2);
			world.setBlock(x + 1, y + yRel, z - 1, Blocks.sandstone, 0, 2);
			world.setBlock(x + 1, y + yRel, z + 1, Blocks.sandstone, 0, 2);
		}

		return true;
	}
}
