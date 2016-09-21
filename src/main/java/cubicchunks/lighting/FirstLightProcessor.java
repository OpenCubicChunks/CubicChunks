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

import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.util.Coords.cubeToMaxBlock;
import static cubicchunks.util.Coords.cubeToMinBlock;
import static cubicchunks.util.Coords.getCubeCenter;
import static cubicchunks.util.Coords.localToBlock;

/**
 * Generates and updates cube initial lighting.
 * <p>
 * Used only when changes are caused by pre-populator terrain generation.
 */
public class FirstLightProcessor implements CubeProcessor {

	private static final int[][] neighborDirections = new int[][]{
			{0, 0, -1},
			{0, 0, 1},
			{0, -1, 0},
			{0, 1, 0},
			{-1, 0, 0},
			{1, 0, 0}
	};
	//mutableBlockPos variable to avoid creating thousands of instances of BlockPos
	private BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
	private ICubeCache cache;

	public FirstLightProcessor(ICubicWorld world) {
		this.cache = world.getCubeCache();
	}

	/**
	 * Requirements: All neighbor cubes exist
	 */
	@Override
	public void calculate(Cube cube) {
		//setRawSkylight is redundant but it allows to get reasonable light values even if neighbor chunks aren't loaded
		setRawSkylight(cube);
		if(diffuseSkylight(cube)) {
			cube.setInitialLightingDone(true);
		}
	}

	private void setRawSkylight(Cube cube) {
		int minBlockX = cubeToMinBlock(cube.getX());
		int maxBlockX = cubeToMaxBlock(cube.getX());

		int minBlockZ = cubeToMinBlock(cube.getZ());
		int maxBlockZ = cubeToMaxBlock(cube.getZ());

		for (int x = minBlockX; x <= maxBlockX; x++) {
			for (int z = minBlockZ; z <= maxBlockZ; z++) {
				//so that it's clearly visible that this value is not set to any value
				int y = Integer.MIN_VALUE;
				mutablePos.setPos(x, y, z);
				setRawSkylightXZ(cube, mutablePos);
			}
		}
	}

	// technically the MutableBlockPos argument is redundant as it's a private field,
	// but not passing any position is just confusing
	private void setRawSkylightXZ(Cube cube, BlockPos.MutableBlockPos blockPos) {
		int cubeY = cube.getY();

		int topBlockY = getHeightmapValue(cache, blockPos.getX(), blockPos.getZ()) - 1;

		int topBlockCubeY = blockToCube(topBlockY);

		if (topBlockCubeY == cubeY) {
			//heightmap is in this cube, so generating this cube may cause light updates below
			columnLightUpdate(cube.getColumn(), blockPos);
		} else if (topBlockCubeY < cubeY) {
			//it's above the heightmap, so this cube didn't change anything and is fully lit at this x/z coords
			cubeSetLight(cube, blockPos, 15);
		} else if (topBlockCubeY == cubeY + 1) {
			//this cube is just 1 cube below the heightmap so it may be affected.
			updateRawLightFromAbove(cube.getColumn(), cube, blockPos);
		}
	}

	private void cubeSetLight(Cube cube, BlockPos.MutableBlockPos pos, int lightValue) {
		for (int y = 0; y < 16; y++) {
			pos.setY(y);
			cube.setLightFor(EnumSkyBlock.SKY, pos, lightValue);
		}
	}

	private void columnLightUpdate(final Column column, BlockPos.MutableBlockPos pos) {
		int topBlockY = getHeightmapValue(column, blockToLocal(pos.getX()), blockToLocal(pos.getZ())) - 1;
		int topBlockCubeY = blockToCube(topBlockY);

		int prevTopBlockY = getHeightmapBelowCubeY(cache, pos.getX(), pos.getZ(), topBlockCubeY);
		int prevTopBlockCubeY = blockToCube(prevTopBlockY);

		Iterable<Cube> subMap = column.getCubes(prevTopBlockCubeY, topBlockCubeY);

		for (Cube cube : subMap) {
			//do we even need to update it?
			if (shouldSkipCube(cube, topBlockCubeY)) {
				continue;
			}
			int cubeY = cube.getY();

			if (cubeY == topBlockCubeY) {
				//scan top-down decreasing light value by max opacity
				int maxOpacity = 0;
				int light = 15;
				for (int localY = 15; localY >= 0; localY--) {
					pos.setY(localToBlock(cubeY, localY));
					int opacity = cube.getBlockState(pos).getLightOpacity(column.getWorld(), pos);
					maxOpacity = Math.max(maxOpacity, opacity);
					light -= maxOpacity;
					if(light < 0) {//only 4 LSB are used, so it can't be < 0
						light = 0;
					}
					cube.setLightFor(EnumSkyBlock.SKY, pos, light);
				}
			} else {
				//mark initial lighting to be re-done
				cube.setInitialLightingDone(false);
			}
		}
	}

	private void updateRawLightFromAbove(Column column, Cube cubeToUpdate, BlockPos.MutableBlockPos pos) {
		int topBlockY = getHeightmapValue(column, blockToLocal(pos.getX()), blockToLocal(pos.getZ())) - 1;
		int light = 15;
		int maxOpacity = 0;
		int cubeYUpperBound = cubeToUpdate.getCoords().getMaxBlockY();

		Cube cubeAbove = column.getCube(cubeToUpdate.getY() + 1);
		assert blockToCube(topBlockY) == cubeAbove.getY();

		pos.setY(topBlockY);
		do {
			//get block and its opacity from correct cube
			IBlockState block = (pos.getY() > cubeYUpperBound ? cubeAbove : cubeToUpdate).getBlockState(pos);
			int opacity = block.getLightOpacity((World) cubeAbove.getWorld(), pos);
			maxOpacity = Math.max(opacity, maxOpacity);
			assert maxOpacity >
					0 : "Inconsistent opacity values: Expected > 0 but got <= 0 (top opaque block is transparent, block = " + block.getBlock().getUnlocalizedName() + ")";
			//once opaque block is hit - always decrease light value
			light -= maxOpacity;
			if(light < 0) {//only 4 LSB are used, so it can't be < 0
				light = 0;
			}
			//only set light in this cube
			if (pos.getY() <= cubeYUpperBound) {
				cubeToUpdate.setLightFor(EnumSkyBlock.SKY, pos, light);
			}
			pos.setY(pos.getY() - 1);
		} while (light > 0);
	}

	private boolean diffuseSkylight(Cube cube) {
		ICubicWorld world = cube.getWorld();

		//cache min/max Y, generating them may be expensive
		int[][] minBlockYArr = new int[16][16];
		int[][] maxBlockYArr = new int[16][16];

		int minBlockX = cubeToMinBlock(cube.getX());
		int maxBlockX = cubeToMaxBlock(cube.getX());

		int minBlockZ = cubeToMinBlock(cube.getZ());
		int maxBlockZ = cubeToMaxBlock(cube.getZ());

		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cache, cube, blockX, blockZ);
				//if there is nothing to update - store contradicting data so we can detect it later
				minBlockYArr[blockX - minBlockX][blockZ - minBlockZ] =
						minMax == null ? Integer.MAX_VALUE : minMax.getLeft();
				maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ] =
						minMax == null ? Integer.MIN_VALUE : minMax.getRight();
			}
		}

		BlockPos.MutableBlockPos pos = mutablePos;

		FastCubeBlockAccess blockAccess = new FastCubeBlockAccess(cache, cube, 1);

		int cubeY = cube.getY();
		int cubeMinBlockY = cubeToMinBlock(cubeY);
		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				pos.setPos(blockX, pos.getY(), blockZ);
				int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				if (minBlockY > maxBlockY) {//is update needed?
					continue;
				}

				for (int y = 0; y < 16; y++) {
					int blockY = y + cubeMinBlockY;

					if (blockY > maxBlockY || blockY < minBlockY) {
						continue;
					}
					pos.setY(blockY);
					if (!needsUpdate(blockAccess, pos)) {
						continue;
					}

					if (!world.checkLightFor(EnumSkyBlock.SKY, pos)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean needsUpdate(FastCubeBlockAccess access, BlockPos.MutableBlockPos pos) {
		if (access.getBlockLightOpacity(pos) >= 15) {
			return false;
		}
		int light = access.getLightFor(EnumSkyBlock.SKY, pos);
		//update only black blocks
		if (light > 0) {
			return false;
		}
		int opacity = access.getBlockLightOpacity(pos);
		for (int[] offset : neighborDirections) {
			pos.setPos(
					pos.getX() + offset[0],
					pos.getY() + offset[1],
					pos.getZ() + offset[2]
			);

			int currentLight = access.getLightFor(EnumSkyBlock.SKY, pos);

			pos.setPos(
					pos.getX() - offset[0],
					pos.getY() - offset[1],
					pos.getZ() - offset[2]
			);
			if (light < Math.max(0, currentLight - opacity - 1)) {
				return true;
			}
		}
		return false;
	}

	private Pair<Integer, Integer> getMinMaxLightUpdateY(ICubeCache cache, Cube cube, int blockX, int blockZ) {
		//getHeightmapValue(x, y, z) = heightOfTopBlock+1
		int heightMax = getHeightmapValue(cache, blockX, blockZ) - 1;//==Y of the top block

		int cubeY = cube.getY();
		//if the top block is below current cube - this cube couldn't change anything
		//because there are no opaque blocks
		if (blockToCube(heightMax) < cubeY) {
			return null;
		}
		if (cubeY < blockToCube(heightMax)) {
			return new ImmutablePair<>(cubeToMinBlock(cubeY), cubeToMaxBlock(cubeY));
		}

		int heightBelowCube = getHeightmapBelowCubeY(cache, blockX, blockZ, cubeY) + 1;
		return new ImmutablePair<>(heightBelowCube, heightMax);
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
		return val == null ? Integer.MIN_VALUE/2 : val;
	}

	private int getHeightmapBelowCubeY(ICubeCache cache, int blockX, int blockZ, int cubeY) {
		int blockY = cubeToMinBlock(cubeY);
		IOpacityIndex index = cache.getColumn(blockToCube(blockX), blockToCube(blockZ)).getOpacityIndex();
		Integer val = index.getTopBlockYBelow(blockToLocal(blockX), blockToLocal(blockZ), blockY);
		return val == null ? Integer.MIN_VALUE/2 : val;
	}

	private boolean canUpdateCube(Cube cube) {
		BlockPos cubeCenter = getCubeCenter(cube);
		final int lightUpdateRadius = 17;
		final int cubeSizeRadius = 8;
		final int bufferRadius = 1;
		final int totalRadius = lightUpdateRadius + cubeSizeRadius + bufferRadius;

		// only continue if the neighbor cubes exist
		return cube.getWorld().testForCubes(cubeCenter, totalRadius, c -> true);
	}

	private boolean shouldSkipCube(Cube cube, int excludedCubeY) {
		//if initial lighting isn't done in this cube yet
		//then lighting in that cube will be calculated later, skip
		return cube.getY() != excludedCubeY && !cube.isInitialLightingDone();
	}
}
