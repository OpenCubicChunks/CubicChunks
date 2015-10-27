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
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

class SkyLightUpdateCalculator {

	/**
	 * Calculates light update for given positions.
	 * @param column Column to calculate light in
	 * @param localX in-column X position
	 * @param localZ in-column Z position
	 * @param minBlockY minimum light update Y. Integer.MIN_VALUE for no lower limit.
	 * @param startBlockY position from which updating should be started. Integer.MAX_VALUE tu update from top of the world.
	 * @return set of affected cube Y positions
	 */
	public Set<Integer> calculate(Column column, int localX, int localZ, int minBlockY, int startBlockY) {
		// NOTE: startBlockY is always the air block above the top block that was added or removed
		World world = column.getWorld();

		if (world.provider.getHasNoSky()) {
			return new HashSet<>(0);
		}

		// did we add or remove sky?
		MutableBlockPos blockPos = new MutableBlockPos(
			Coords.localToBlock(column.xPosition, localX),
			startBlockY - 1,
			Coords.localToBlock(column.zPosition, localZ)
		);
		Integer newMaxBlockY = column.getSkylightBlockY(localX, localZ);
		int maxCubeY = Coords.blockToCube(newMaxBlockY == null ? Integer.MIN_VALUE : newMaxBlockY);

		Set<Integer> cubesToDiffuse = new HashSet<>();

		//attempt to update lighting only in loaded cubes
		for(Cube cube : column.getCubes()) {
			int cubeY = cube.getY();
			int minCubeBlockY = cubeY * 16;

			//do we even need to do anything here?
			if(startBlockY < minCubeBlockY) {
				continue;
			}
			if(cubeY > maxCubeY) {
				//if light value at the bottom is already correct - nothing to do here
				blockPos.setBlockPos(localX, 0, localZ);
				if(cube.getLightValue(EnumSkyBlock.SKY, blockPos) == 15) {
					continue;
				}
				//we are above top block - set to light level = 15
				for(int y = 0; y < 16; y++) {
					blockPos.setBlockPos(localX, y, localZ);
					cube.setLightValue(EnumSkyBlock.SKY, blockPos, 15);
				}
				cubesToDiffuse.add(cube.getY());
			} else if(/*cubeY == maxCubeY - 1 || */cubeY == maxCubeY) {
				//we actually also attempt to update cube at maxCubeY - 1 here
				int light = 15, maxOpacity = 0;

				//TODO: remove code duplication here
				for(int y = 15; y >=0; y--) {
					blockPos.setBlockPos(localX, y, localZ);
					maxOpacity = Math.max(maxOpacity, cube.getBlockAt(blockPos).getLightOpacity());
					light -= maxOpacity;
					if(light < 0) light = 0;

					cube.setLightValue(EnumSkyBlock.SKY, blockPos, light);
				}

				cubesToDiffuse.add(cube.getY());

				//light can propagate to cube below too
				if((cube = column.getCube(maxCubeY - 1)) != null) {
					for(int y = 15; y >=0; y--) {
						blockPos.setBlockPos(localX, y, localZ);
						maxOpacity = Math.max(maxOpacity, cube.getBlockAt(blockPos).getLightOpacity());
						light -= maxOpacity;
						if(light < 0) light = 0;

						cube.setLightValue(EnumSkyBlock.SKY, blockPos, light);
					}
					cubesToDiffuse.add(cube.getY());
				}
			} else if(cubeY == maxCubeY - 1) {
				//it's done in code above
				continue;
			} else {
				assert cubeY < maxCubeY - 1;
				blockPos.setBlockPos(localX, 15, localZ);
				//if we are below minBlockY or if the top block has correct light value (0) - nothing to do
				if(minCubeBlockY+15 < minBlockY || cube.getLightValue(EnumSkyBlock.SKY, blockPos) == 0) {
					continue;
				}
				do {
					cube.setLightValue(EnumSkyBlock.SKY, blockPos, 0);
					blockPos.y--;
				}while(blockPos.y >= 0);

				cubesToDiffuse.add(cube.getY());
			}
		}

		return cubesToDiffuse;
	}
}
