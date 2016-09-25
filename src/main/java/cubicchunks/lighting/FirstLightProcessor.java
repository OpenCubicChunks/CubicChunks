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

import cubicchunks.util.CubeCoords;
import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
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
 * Generates and updates cube initial lighting, and propagates light changes
 * caused by generating cube downwards.
 * <p>
 * Used only when changes are caused by pre-populator terrain generation.
 * <p>
 * THIS SHOULD ONLY EVER BE USED ONCE PER CUBE.
 */
//TODO: make it also update blocklight
public class FirstLightProcessor {
	//mutableBlockPos variable to avoid creating thousands of instances of BlockPos
	private BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
	private ICubeCache cache;

	public FirstLightProcessor(ICubicWorld world) {
		this.cache = world.getCubeCache();
	}

	public void setRawSkylight(Cube cube) {
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
			blockColumnLightUpdate(cube.getColumn(), blockPos);
		} else if (topBlockCubeY < cubeY) {
			//it's above the heightmap, so this cube didn't change anything and is fully lit at this x/z coords
			setLightInCubeBlockColumn(cube, blockPos, 15);
		} else if (topBlockCubeY == cubeY + 1) {
			//this cube is just 1 cube below the heightmap so it may be affected by direct sunlight
			updateRawLightFromAbove(cube.getColumn(), cube, blockPos);
		} else {
			setLightInCubeBlockColumn(cube, blockPos, 0);
		}
	}

	private void setLightInCubeBlockColumn(Cube cube, BlockPos.MutableBlockPos pos, int lightValue) {
		for (int y = 0; y < 16; y++) {
			pos.setY(y);
			cube.setLightFor(EnumSkyBlock.SKY, pos, lightValue);
		}
	}

	private void blockColumnLightUpdate(final Column column, BlockPos.MutableBlockPos pos) {
		int topBlockY = getHeightmapValue(column, blockToLocal(pos.getX()), blockToLocal(pos.getZ())) - 1;
		int topBlockCubeY = blockToCube(topBlockY);

		int prevTopBlockY = getHeightmapBelowCubeY(cache, pos.getX(), pos.getZ(), topBlockCubeY);
		int prevTopBlockCubeY = blockToCube(prevTopBlockY);

		Iterable<Cube> subMap = column.getCubes(prevTopBlockCubeY, topBlockCubeY);

		for (Cube cube : subMap) {
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
					if (light < 0) {//only 4 LSB are used, so it can't be < 0
						light = 0;
					}
					cube.setLightFor(EnumSkyBlock.SKY, pos, light);
				}
			} else if (cubeY + 1 == topBlockCubeY) {
				updateRawLightFromAbove(column, cube, pos);
			} else {
				setLightInCubeBlockColumn(cube, pos, 0);
			}
		}
	}

	private void updateRawLightFromAbove(Column column, Cube cubeToUpdate, BlockPos.MutableBlockPos pos) {
		int topBlockY = getHeightmapValue(column, blockToLocal(pos.getX()), blockToLocal(pos.getZ())) - 1;
		int light = 15;
		int maxOpacity = 0;
		//if the save isn't corrupted checking this is redundant, but it's very easy for it to end up wrong
		int cubeYLowerBound = cubeToUpdate.getCoords().getMinBlockY();
		int cubeYUpperBound = cubeToUpdate.getCoords().getMaxBlockY();

		Cube cubeAbove = column.getCube(cubeToUpdate.getY() + 1);
		if(cubeAbove == null) {
			//that's the best we can do if the cube above isn't loaded
			setLightInCubeBlockColumn(cubeToUpdate, pos, 0);
			return;
		}
		assert blockToCube(topBlockY) == cubeAbove.getY();

		pos.setY(topBlockY);
		do {
			Cube cube = pos.getY() > cubeYUpperBound ? cubeAbove : cubeToUpdate;
			//get block and its opacity from correct cube
			IBlockState block = cube.getBlockState(pos);
			int opacity = block.getLightOpacity((World) cube.getWorld(), pos);
			maxOpacity = Math.max(opacity, maxOpacity);
			//once opaque block is hit - always decrease light value
			light -= maxOpacity;
			if (light < 0) {//only 4 LSB are used, so it can't be < 0
				light = 0;
			}
			//only set light in this cube
			if (pos.getY() <= cubeYUpperBound) {
				cubeToUpdate.setLightFor(EnumSkyBlock.SKY, pos, light);
			}
			pos.setY(pos.getY() - 1);
		} while (pos.getY() >= cubeYLowerBound);
	}

	public boolean diffuseSkylight(Cube generatedCube) {
		ICubicWorld world = generatedCube.getWorld();

		//cache min/max Y, generating them may be expensive
		int[][] minBlockYArr = new int[16][16];
		int[][] maxBlockYArr = new int[16][16];

		int minBlockX = cubeToMinBlock(generatedCube.getX());
		int maxBlockX = cubeToMaxBlock(generatedCube.getX());

		int minBlockZ = cubeToMinBlock(generatedCube.getZ());
		int maxBlockZ = cubeToMaxBlock(generatedCube.getZ());

		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cache, generatedCube, blockX, blockZ);
				//if there is nothing to update - store contradicting data so we can detect it later
				minBlockYArr[blockX - minBlockX][blockZ - minBlockZ] =
						minMax == null ? Integer.MAX_VALUE : minMax.getLeft();
				maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ] =
						minMax == null ? Integer.MIN_VALUE : minMax.getRight();
			}
		}

		BlockPos.MutableBlockPos pos = mutablePos;

		//can we update the current cube? if not, stop now
		if(!tryUpdateCurrentCube(generatedCube, minBlockYArr, maxBlockYArr,
				minBlockX, maxBlockX, minBlockZ, maxBlockZ, pos)) {
			return false;
		}
		Column column = generatedCube.getColumn();
		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				pos.setPos(blockX, pos.getY(), blockZ);
				int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				if (minBlockY > maxBlockY) {//is update needed?
					continue;
				}

				Iterable<Cube> cubes = column.getCubes(blockToCube(minBlockY), blockToCube(maxBlockY));
				for(Cube cube : cubes) {
					//this one has been handled before this loop
					if(cube == generatedCube) {
						continue;
					}
					int cubeY = cube.getY();
					//is the update even possible?
					if(!canUpdateCube(cube)) {
						//schedule updates for later, that's the best we can do
						//cancelling everything and recalculating later is a bad idea
						//because it's potentially very slow
						int minScheduledY = Math.max(cubeToMinBlock(cubeY), minBlockY);
						int maxScheduledY = Math.min(cubeToMaxBlock(cubeY), maxBlockY);
						world.getLightingManager().queueDiffuseUpdate(cube, pos.getX(), pos.getZ(), minScheduledY, maxScheduledY);
						continue;
					}

					if (!diffuseBlockColumnInCube(cube, minBlockY, maxBlockY, pos)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean tryUpdateCurrentCube(Cube cube, int[][] minBlockYArr, int[][] maxBlockYArr,
	                                     int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ,
	                                     BlockPos.MutableBlockPos pos) {
		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				pos.setPos(blockX, pos.getY(), blockZ);
				int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				if(!diffuseBlockColumnInCube(cube, minBlockY, maxBlockY, pos)) {
					return false;
				}
			}
		}
		return true;

	}

	private boolean diffuseBlockColumnInCube(Cube cube, int minBlockY, int maxBlockY, BlockPos.MutableBlockPos pos) {
		ICubicWorld world = cube.getWorld();
		int cubeMinBlockY = cubeToMinBlock(cube.getY());
		FastCubeBlockAccess blockAccess = new FastCubeBlockAccess(cache, cube, 1);
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
		return true;
	}

	private boolean needsUpdate(FastCubeBlockAccess access, BlockPos.MutableBlockPos pos) {
		//opaque blocks don't eed update. Nothing can emit skylight, and skylight can't get into them nor out of them
		if (access.getBlockLightOpacity(pos) >= 15) {
			return false;
		}
		int light = access.getLightFor(EnumSkyBlock.SKY, pos);
		//update only black blocks - blocks that light can get into
		if (light > 0) {
			return false;
		}
		//is there any neighbor with higher light value?
		int opacity = access.getBlockLightOpacity(pos);
		for (EnumFacing facing : EnumFacing.values()) {
			pos.move(facing);
			int currentLight = access.isDirectlyExposedToSkylight(pos) ? 15 : 0;
			pos.move(facing.getOpposite());

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
		//if the top block is below current cube - Everything is fully lit, no update needed
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

	private int getTopBlockYInCube(Cube cube, BlockPos.MutableBlockPos pos) {ICubicWorld world = cube.getWorld();
		CubeCoords cubePos = cube.getCoords();
		pos.setY(cubePos.getMaxBlockY());
		int minBlockY = cubePos.getMinBlockY();
		int topY = minBlockY - 1;
		//scan this cube top down
		//Is it faster than binary search through OpacityIndex?
		while (pos.getY() >= minBlockY) {
			IBlockState block = cube.getBlockState(pos);
			if(block.getLightOpacity((World)world, pos) > 0) {
				topY = pos.getY();
				break;
			}
			pos.setY(pos.getY() - 1);
		}
		return topY;
	}
}
