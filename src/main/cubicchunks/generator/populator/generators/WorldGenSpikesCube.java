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
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.world.World;

public class WorldGenSpikesCube extends WorldGeneratorCube {
	private Block block;

	public WorldGenSpikesCube(Block block) {
		this.block = block;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		// This is part of The End generator.
		if (!world.isAirBlock(x, y, z) || world.getBlock(x, y - 1, z) != this.block) {
			return false;

		}

		// this is a high structure and may populate aditional cubes (or not
		// fully generate)
		int height = rand.nextInt(32) + 6;
		int radius = rand.nextInt(4) + 1;

		for (int xAbs = x - radius; xAbs <= x + radius; ++xAbs) {
			for (int zAbs = z - radius; zAbs <= z + radius; ++zAbs) {
				int xDist = xAbs - x;
				int zDist = zAbs - z;

				if (xDist * xDist + zDist * zDist <= radius * radius + 1
						&& world.getBlock(xAbs, y - 1, zAbs) != this.block) {
					return false;
				}
			}
		}

		for (int yAbs = y; yAbs < y + height; ++yAbs) {
			for (int xAbs = x - radius; xAbs <= x + radius; ++xAbs) {
				for (int zAbs = z - radius; zAbs <= z + radius; ++zAbs) {
					int xDist = xAbs - x;
					int zDist = zAbs - z;

					if (xDist * xDist + zDist * zDist <= radius * radius + 1) {
						world.setBlock(xAbs, yAbs, zAbs, Blocks.obsidian, 0, 2);
					}
				}
			}
		}

		EntityEnderCrystal enderCrystal = new EntityEnderCrystal(world);
		enderCrystal.setLocationAndAngles(x + 0.5F, y + height, z + 0.5F, rand.nextFloat() * 360.0F, 0.0F);
		world.spawnEntityInWorld(enderCrystal);
		world.setBlock(x, y + height, z, Blocks.bedrock, 0, 2);
		return true;
	}
}
