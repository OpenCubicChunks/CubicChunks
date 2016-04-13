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

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.WorldContext;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import static cubicchunks.util.Coords.*;
import static cubicchunks.util.MathUtil.min;

public class FirstLightProcessor extends CubeProcessor {

	//mutableBlockPos variable to avoid creating thousands of instances of BlockPos
	private MutableBlockPos mutablePos = new MutableBlockPos();

	public FirstLightProcessor(String name, ICubeCache cache, int batchSize) {
		super(name, cache, batchSize);
	}

	@Override
	public boolean calculate(Cube cube) {
		WorldContext worldContext = WorldContext.get(cube.getWorld());

		if (!canUpdateCube(cube, worldContext)) {
			return false;
		}

		int minBlockX = cubeToMinBlock(cube.getX()) - 1;
		int maxBlockX = cubeToMaxBlock(cube.getX()) + 1;

		int minBlockZ = cubeToMinBlock(cube.getZ()) - 1;
		int maxBlockZ = cubeToMaxBlock(cube.getZ()) + 1;

		ICubeCache cache = worldContext.getCubeCache();

		setRawSkylight(cache, cube, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
		diffuseSkylight(cache, worldContext, cube, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
		return true;
	}

	private void setRawSkylight(ICubeCache cache, Cube cube, int minX, int maxX, int minZ, int maxZ) {
		for (mutablePos.x = minX; mutablePos.x <= maxX; mutablePos.x++) {
			for (mutablePos.z = minZ; mutablePos.z <= maxZ; mutablePos.z++) {
				//so that it's clearly visible that this value is not set to any value
				mutablePos.y = Integer.MIN_VALUE;
				setRawSkylightXZ(cube, mutablePos);
			}
		}
	}

	// technically the MutableBlockPos argument is redundant as it's a private field,
	// but not passing any position is just confusing
	private void setRawSkylightXZ(Cube cube, MutableBlockPos pos) {

		int cubeY = cube.getY();

		int localX = blockToLocal(pos.getX());
		int localZ = blockToLocal(pos.getZ());

		// compute bounds on the sky light gradient
		int topBlockY = getHeightmapValue(cube.getColumn(), localX, localZ) - 1;
		int topBlockCubeY = blockToCube(topBlockY);

		if (topBlockCubeY == cubeY) {
			//heightmap is in this cube, so generating this cube causes light updates may cause light updates below
			columnLightUpdate(cube.getColumn(), pos, localX, topBlockY, localZ);
		} else if (topBlockCubeY < cubeY) {
			//it's above the heightmap, so this cube didn't change anything ans is fully lit at this x/z coords
			cubeSetLight(cube, pos, 15);
		} else if (topBlockCubeY == cubeY + 1) {
			//it's 1 cube below heightmap, so  there is a chance that light will reach this cube
			updateCubeLight(cube, pos, localX, topBlockY, localZ);
		} else {
			assert topBlockCubeY > cubeY + 1;
			//there is no chance that raw skylight will reach this cube, set to dark
			cubeSetLight(cube, pos, 0);
		}
	}

	private void cubeSetLight(Cube cube, MutableBlockPos pos, int lightValue) {
		for (int y = 0; y < 16; y++) {
			pos.y = y;
			cube.setLightFor(EnumSkyBlock.SKY, pos, lightValue);
		}
	}

	private void updateCubeLight(Cube cube, MutableBlockPos pos, int localX, int topBlockY, int localZ) {
		Column column = cube.getColumn();
		IOpacityIndex index = column.getOpacityIndex();

		int light = 15;
		int maxOpacity = 0;

		int y = topBlockY;

		int minCubeY = cubeToMinBlock(cube.getY());
		int maxCubeY = cubeToMaxBlock(cube.getY());

		assert index.getOpacity(localX, y, localZ) != 0 :
				"Top non-transparent block is transparent! Column coords=" +
						column.getChunkCoordIntPair() + ", localX=" + localX + ", localZ=" + localZ + ",y= " + y;

		//set everything above to max light value
		for (int blockY = maxCubeY; blockY > y; blockY--) {
			pos.y = blockY;
			cube.setLightFor(EnumSkyBlock.SKY, pos, 15);
		}

		//we only need to go down to the cubeMinY block
		//we start above the cube, and light value will decrease at least by ne each step
		//even if we start at the top of the cube, it will be dark at the time we reach the bottom
		//and we need to set light for the whole Y range, so we can't stop earlier
		while (y >= minCubeY) {
			int opacity = index.getOpacity(localX, y, localZ);
			//when we reach opaque block - light value should start decreasing
			if (maxOpacity < opacity) {
				maxOpacity = opacity;
			}
			light -= maxOpacity;
			//don't set negative light values
			if (light < 0) {
				light = 0;
			}
			pos.y = y;
			if (blockToCube(y) == cube.getY()) {
				cube.setLightFor(EnumSkyBlock.SKY, pos, light);
			}
			y--;
		}
		assert light == 0 : "Expected light value 0, got " + light;
	}

	private void columnLightUpdate(final Column column, MutableBlockPos pos, final int localX, final int topBlockY, final int localZ) {
		int topBlockCubeY = blockToCube(topBlockY);

		for (Cube cube : column.getCubes()) {
			int cubeY = cube.getY();

			//is there anything to update?
			if (cubeY > topBlockCubeY) {
				continue;
			}
			//can skylight reach this cube?
			if (cubeY == topBlockCubeY) {
				//if so, do standard cube light update
				updateCubeLight(cube, pos, localX, topBlockY, localZ);
				continue;
			}
			//raw skylight can't reach this cube, it's dark
			cubeSetLight(cube, pos, 0);
		}
	}

	private void diffuseSkylight(ICubeCache cache, WorldContext worldContext, Cube cube, int minX, int maxX, int minZ, int maxZ) {
		World world = cube.getWorld();

		//we need to go to neighbor column to fully update everything
		minX--;
		maxX++;

		minZ--;
		maxZ++;
		for (mutablePos.x = minX; mutablePos.x <= maxX; mutablePos.x++) {
			for (mutablePos.z = minZ; mutablePos.z <= maxZ; mutablePos.z++) {
				diffuseBlockColumn(world, worldContext, cache, cube, mutablePos);
			}
		}
	}

	private void diffuseBlockColumn(World world, WorldContext context, ICubeCache cache, Cube cube, MutableBlockPos pos) {
		/**
		 * 2d example of what happens here:
		 * ===
		 * ### - block
		 * ===
		 * ...
		 * ... - air
		 * ...
		 * ;;;
		 * ;;; - dark air
		 * ;;;
		 *
		 * x-1 x x+1
		 * ...===...
		 * ...###...
		 * ...===...
		 * ...;;;...
		 * ...;;;...
		 * ...;;;...
		 * ===;;;...
		 * ###;;;...
		 * ===;;;...
		 * =========
		 * #########
		 * =========
		 *
		 * In this case light should spread from lit areas from x-1 and x+1 to x.
		 * The minimum Y that light can directly spread to is at [min(heightAt(x-1), heightAt(x+1)) + 1]
		 * And the maximum Y that light can directly spread to is [heightAt(x) - 1].
		 * When all surrounding heights are higher, light can't spread here, so nothing happens.
		 *
		 * We don't need to care about cases when light spreads to urrent block indirectly,
		 * It needs to spread to some other block directly for that to happen, and world.checkLightFor()
		 * recalculates full light spread.
		 *
		 * Note that world.checkLightFor() doesn't cause light to spread to/from (x, y, z) if light value
		 * in this block is already correct. It only updates if light value isn't equal to raw light value
		 * which is max of light at (x, y, z), and [light values from surrounding blocks - 1].
		 *
		 * In this state all non-lit blocks should have light values 0,
		 * so we need to care only about blocks  light can spready to directly from surrounding blocks.
		 */

		Column column = cube.getColumn();

		//heightAt(x, y, z) = heigtmap(x, y, z)-1, and from heightAt we need to subtract 1.
		int heightMax = getHeightmapValue(cache, pos.x, pos.z) - 1 - 1;

		int heightmap1 = getHeightmapValue(cache, pos.x, pos.z + 1);
		int heightmap2 = getHeightmapValue(cache, pos.x, pos.z - 1);
		int heightmap3 = getHeightmapValue(cache, pos.x + 1, pos.z);
		int heightmap4 = getHeightmapValue(cache, pos.x - 1, pos.z);

		//heightmap is already block above top block, don't add 1
		int heightMin = min(heightmap1, heightmap2, heightmap3, heightmap4);

		if (heightMin > heightMax) {
			return;
		}

		int minCubeY = blockToCube(heightMin);
		int maxCubeY = blockToCube(heightMax);

		//difference between
		for (Cube c : column.getCubes()) {
			int cubeY = c.getY();
			int cubeMinY = cubeToMinBlock(cubeY);
			//do we need to do anything in this cube?
			if (cubeY > maxCubeY || cubeY < minCubeY) {
				continue;
			}
			//can update here?
			if (!canUpdateCube(cube, context)) {
				//schedule update
				context.getLightingManager().queueDiffuseUpdate(
						cube,
						Coords.blockToLocal(pos.x),
						Coords.blockToLocal(pos.z),
						minCubeY, maxCubeY
				);
			} else {
				for (int y = 0; y < 16; y++) {
					pos.y = y + cubeMinY;
					world.checkLightFor(EnumSkyBlock.SKY, pos);
				}
			}
		}
	}

	private int getHeightmapValue(ICubeCache cache, int blockX, int blockZ) {
		int cubeX = blockToCube(blockX);
		int cubeZ = blockToCube(blockZ);

		int localX = blockToLocal(blockX);
		int localZ = blockToLocal(blockZ);

		Column column = cache.getColumn(cubeX, cubeZ);

		return getHeightmapValue(column, localX, localZ);
	}

	private int getHeightmapValue(Column column, int localX, int localZ) {
		Integer val = column.getHeightmapAt(localX, localZ);
		return val == null ? Integer.MIN_VALUE : val;
	}

	private boolean canUpdateCube(Cube cube, WorldContext worldContext) {
		BlockPos cubeCenter = getCubeCenter(cube);
		final int lightUpdateRadius = 18;
		final int cubeSizeRadius = 8;

		// only continue if the neighboring cubes are at least in the lighting stage
		return worldContext.blocksExist(cubeCenter, lightUpdateRadius + cubeSizeRadius, true, GeneratorStage.LIGHTING);
	}
}
