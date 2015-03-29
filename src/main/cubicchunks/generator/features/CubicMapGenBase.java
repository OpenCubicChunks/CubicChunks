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
package cubicchunks.generator.features;

import java.util.Random;

import net.minecraft.world.World;
import cubicchunks.world.Cube;

public abstract class CubicMapGenBase {
	
	/** The number of Chunks to gen-check in any given direction. */
	protected int m_range = 8;
	
	/** The RNG used by the MapGen classes. */
	protected Random rand = new Random();
	
	/** This world object. */
	protected World m_world;
	
	public void generate(World world, Cube cube) {
		int xOrigin = cube.getX();
		int yOrigin = cube.getY();
		int zOrigin = cube.getZ();
		
		int radius = this.m_range;
		this.m_world = world;
		this.rand.setSeed(world.getSeed());
		long randX = this.rand.nextLong();
		long randY = this.rand.nextLong();
		long randZ = this.rand.nextLong();
		
		for (int x = xOrigin - radius; x <= xOrigin + radius; ++x) {
			for (int y = yOrigin - radius; y <= yOrigin + radius; ++y) {
				for (int z = zOrigin - radius; z <= zOrigin + radius; ++z) {
					long randX_mul = (long)x * randX;
					long randY_mul = (long)y * randY;
					long randZ_mul = (long)z * randZ;
					this.rand.setSeed(randX_mul ^ randY_mul ^ randZ_mul ^ world.getSeed());
					this.generate(world, cube, x, y, z, xOrigin, yOrigin, zOrigin);
				}
			}
			
		}
	}
	
	protected abstract void generate(World world, Cube cube, int x, int y, int z, int xOrig, int yOrig, int zOrig);
}
