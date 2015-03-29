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
 ******************************************************************************/
package cubicchunks.lighting;

import net.minecraft.block.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import cubicchunks.CubeWorld;
import cubicchunks.util.Coords;
import cubicchunks.world.Column;
import cubicchunks.world.Cube;

public class SkyLightUpdateCalculator {
	
	public void calculate(Column column, int localX, int localZ, int minBlockY, int maxBlockY) {
		// NOTE: maxBlockY is always the air block above the top block that was added or removed
		
		World world = column.getWorld();
		LightingManager lightingManager = CubeWorld.getLightingManager(world);
		
		if (world.dimension.hasNoSky()) {
			return;
		}
		
		// did we add or remove sky?
		boolean addedSky = column.getBlockState(new BlockPos(localX, maxBlockY - 1, localZ)).getBlock() == Blocks.AIR;
		int newMaxBlockY = addedSky ? minBlockY : maxBlockY;
		
		// reset sky light for the affected y range
		int lightValue = addedSky ? 15 : 0;
		for (int blockY = minBlockY; blockY < maxBlockY; blockY++) {
			// save the light value
			int cubeY = Coords.blockToCube(blockY);
			Cube cube = column.getCube(cubeY);
			if (cube != null) {
				int localY = Coords.blockToLocal(blockY);
				cube.setLightValue(LightType.SKY, new BlockPos(localX, localY, localZ), lightValue);
			}
		}
		
		// compute the skylight falloff starting at the new top block
		lightValue = 15;
		int bottomBlockY = Coords.cubeToMinBlock(column.getBottomCubeY());
		for (int blockY = newMaxBlockY - 1; blockY > bottomBlockY; blockY--) {
			// get the opacity to apply for this block
			int lightOpacity = Math.max(1, column.getBlockState(new BlockPos(localX, blockY, localZ)).getBlock().getOpacity());
			
			// compute the falloff
			lightValue = Math.max(lightValue - lightOpacity, 0);
			
			// save the light value
			int cubeY = Coords.blockToCube(blockY);
			Cube cube = column.getCube(cubeY);
			if (cube != null) {
				int localY = Coords.blockToLocal(blockY);
				cube.setLightValue(LightType.SKY, new BlockPos(localX, localY, localZ), lightValue);
			}
			
			if (lightValue == 0) {
				// we ran out of light
				break;
			}
		}
		
		// update this block and its xz neighbors
		int blockX = Coords.localToBlock(column.chunkX, localX);
		int blockZ = Coords.localToBlock(column.chunkZ, localZ);
		diffuseSkyLightForBlockColumn(lightingManager, blockX - 1, blockZ, minBlockY, maxBlockY);
		diffuseSkyLightForBlockColumn(lightingManager, blockX + 1, blockZ, minBlockY, maxBlockY);
		diffuseSkyLightForBlockColumn(lightingManager, blockX, blockZ - 1, minBlockY, maxBlockY);
		diffuseSkyLightForBlockColumn(lightingManager, blockX, blockZ + 1, minBlockY, maxBlockY);
		diffuseSkyLightForBlockColumn(lightingManager, blockX, blockZ, minBlockY, maxBlockY);
	}
	
	private void diffuseSkyLightForBlockColumn(LightingManager lightingManager, int blockX, int blockZ, int minBlockY, int maxBlockY) {
		for (int blockY = minBlockY; blockY < maxBlockY; blockY++) {
			lightingManager.computeDiffuseLighting(new BlockPos(blockX, blockY, blockZ), LightType.SKY);
		}
	}
}
