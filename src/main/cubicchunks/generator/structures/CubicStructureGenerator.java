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
package cubicchunks.generator.structures;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import cubicchunks.world.cube.Cube;

public abstract class CubicStructureGenerator {
	
	/** The number of Chunks to gen-check in any given direction. */
	protected int range = 8;
	
	/** The RNG used by the MapGen classes. */
	protected Random rand = new Random();
	
	/** This world object. */
	protected World m_world;
	
	public void generate(World world, Cube cube) {
		int xOrigin = cube.getX();
		int yOrigin = cube.getY();
		int zOrigin = cube.getZ();
		
		int radius = this.range;
		this.m_world = world;
		this.rand.setSeed(world.getSeed());
		long randX = this.rand.nextLong();
		long randY = this.rand.nextLong();
		long randZ = this.rand.nextLong();
		
		for (int x = xOrigin - radius; x <= xOrigin + radius; ++x) {
			for (int y = yOrigin - radius; y <= yOrigin + radius; ++y) {
				for (int z = zOrigin - radius; z <= zOrigin + radius; ++z) {
					long randX_mul = x * randX;
					long randY_mul = y * randY;
					long randZ_mul = z * randZ;
					this.rand.setSeed(randX_mul ^ randY_mul ^ randZ_mul ^ world.getSeed());
					this.generate(world, cube, x, y, z, xOrigin, yOrigin, zOrigin);
				}
			}
			
		}
	}
	
	protected abstract void generate(World world, Cube cube, int x, int y, int z, int xOrig, int yOrig, int zOrig);
	
	protected abstract void generateNode(Cube cube, long seed, int xOrigin, int yOrigin, int zOrigin, double x, double y, double z, float size_base, float curve, float angle, int numTry, int tries, double yModSinMultiplier);

	protected boolean scanForLiquid(Cube cube, int xDist1, int xDist2, int yDist1, int yDist2, int zDist1,
			int zDist2, Block stationaryLiquid, Block flowingLiquid) {
				boolean result = false;
				for (int x1 = xDist1; !result && x1 < xDist2; ++x1) {
					for (int z1 = zDist1; !result && z1 < zDist2; ++z1) {
						for (int y1 = yDist2; !result && y1 >= yDist1; --y1) {
							Block block = cube.getBlockState(new BlockPos(x1, y1, z1)).getBlock();
			
							if (y1 < 0 || y1 >= 16)
							{
								continue;
							}
							if (block == stationaryLiquid || block == flowingLiquid) {
								result = true;
							}
			
							if (y1 != yDist1 - 1 && x1 != xDist1 && x1 != xDist2 - 1 && z1 != zDist1 && z1 != zDist2 - 1) {
								y1 = yDist1;
							}
						}
					}
				}
				return result;
			}

	protected double calculateDistance(int origin, int x1, double x, double modSin) {
		return (x1 + origin * 16 + 0.5D - x) / modSin;
	}
}
