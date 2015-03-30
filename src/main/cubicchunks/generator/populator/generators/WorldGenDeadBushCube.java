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
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class WorldGenDeadBushCube extends WorldGeneratorCube {
	private final Block blockType;

	public WorldGenDeadBushCube(Block blockType) {
		this.blockType = blockType;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		Block block;

		int minY = getMinBlockYFromRandY(y);

		while (((block = world.getBlock(x, y, z)).getMaterial() == Material.air || block.getMaterial() == Material.leaves)) {
			if (--y < minY) {
				return false;
			}
		}

		boolean generated = false;

		for (int i = 0; i < 4; ++i) {
			int xAbs = x + rand.nextInt(8) - rand.nextInt(8);
			int yAbs = y + rand.nextInt(4) - rand.nextInt(4);
			int zAbs = z + rand.nextInt(8) - rand.nextInt(8);

			if (world.isAirBlock(xAbs, yAbs, zAbs) && this.blockType.canBlockStay(world, xAbs, yAbs, zAbs)) {
				world.setBlock(xAbs, yAbs, zAbs, this.blockType, 0, 2);
				generated = true;
			}
		}

		return generated;
	}
}
