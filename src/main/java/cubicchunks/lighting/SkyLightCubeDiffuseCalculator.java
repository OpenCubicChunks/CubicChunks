/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.lighting;

import cubicchunks.util.Coords;
import cubicchunks.util.MutableBlockPos;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

class SkyLightCubeDiffuseCalculator {

	private final MutableBlockPos pos = new MutableBlockPos();

	boolean calculate(Column column, int localX, int localZ, int cubeY) {
		if(column instanceof BlankColumn || column.getCube(cubeY) == null) {
			return true;
		}
		// update this block and its xz neighbors
		int blockX = Coords.localToBlock(column.xPosition, localX);
		int blockZ = Coords.localToBlock(column.zPosition, localZ);

		World world = column.getWorld();

		int minY = Coords.cubeToMinBlock(cubeY);
		int maxY = Coords.cubeToMaxBlock(cubeY);
		pos.setBlockPos(blockX, minY, blockZ);
		if(!world.isAreaLoaded(pos, 18, false)) {
			return false;
		}
		pos.y = maxY;
		if(!world.isAreaLoaded(pos, 18, false)) {
			return false;
		}

		boolean updated = true;
		updated &= diffuseSkyLightForBlockColumn(world, blockX - 1, blockZ, cubeY);
		updated &= diffuseSkyLightForBlockColumn(world, blockX + 1, blockZ, cubeY);
		updated &= diffuseSkyLightForBlockColumn(world, blockX, blockZ - 1, cubeY);
		updated &= diffuseSkyLightForBlockColumn(world, blockX, blockZ + 1, cubeY);
		updated &= diffuseSkyLightForBlockColumn(world, blockX, blockZ, cubeY);

		assert updated;
		column.setModified(true);
		return true;
	}

	private boolean diffuseSkyLightForBlockColumn(World world, int blockX, int blockZ, int cubeY) {
		boolean ok = true;
		for (int y = 0; y < 16; y++) {
			pos.setBlockPos(blockX, y + cubeY * 16, blockZ);
			ok &= world.checkLightFor(EnumSkyBlock.SKY, pos);
		}
		return ok;
	}
}
