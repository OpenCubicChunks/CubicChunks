/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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
package cubicchunks.util;

import cubicchunks.world.cube.Cube;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class Coords {
	public static final int CUBE_SIZE = 16;
	
	public static final int CUBE_MAX_X = CUBE_SIZE;
	public static final int CUBE_MAX_Y = CUBE_SIZE;
	public static final int CUBE_MAX_Z = CUBE_SIZE;
	
	public static final int HALF_CUBE_MAX_X = CUBE_MAX_X / 2;
	public static final int HALF_CUBE_MAX_Y = CUBE_MAX_Y / 2;
	public static final int HALF_CUBE_MAX_Z = CUBE_MAX_Z / 2;

	public static int blockToLocal(int val) {
		return val & 0xf;
	}

	public static int blockToCube(int val) {
		return val >> 4;
	}

	public static int localToBlock(int cubeVal, int localVal) {
		return cubeToMinBlock(cubeVal) + localVal;
	}

	public static int cubeToMinBlock(int val) {
		return val << 4;
	}

	public static int cubeToMaxBlock(int val) {
		return cubeToMinBlock(val) + 15;
	}

	public static int getCubeXForEntity(Entity entity) {
		return blockToCube(MathHelper.floor_double(entity.posX));
	}

	public static int getCubeZForEntity(Entity entity) {
		return blockToCube(MathHelper.floor_double(entity.posZ));
	}

	public static int getCubeYForEntity(Entity entity) {
		// the entity is in the cube it's inside, not the cube it's standing on
		return blockToCube(MathHelper.floor_double(entity.posY));
	}

	public static BlockPos getCubeCenter(Cube cube) {
		return new BlockPos(
			cubeToMinBlock(cube.getX()) + 8, 
			cubeToMinBlock(cube.getY()) + 8,
			cubeToMinBlock(cube.getZ()) + 8
		);
	}
}