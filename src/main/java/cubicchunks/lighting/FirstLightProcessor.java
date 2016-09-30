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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.IntHash;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.util.Coords.cubeToMaxBlock;
import static cubicchunks.util.Coords.cubeToMinBlock;
import static cubicchunks.util.Coords.getCubeCenter;

/**
 *
 * Notes on world.checkLightFor():
 * Decreasing light value:
 * Light is recalculated starting from 0 ONLY for blocks where rawLightValue
 * is equal to savedLightValue (ie. updating skylight source that is not there anymore). Otherwise existing light values are assumed to be correct.
 * Generates and updates cube initial lighting, and propagates light changes
 * caused by generating cube downwards.
 * <p>
 * Used only when changes are caused by pre-populator terrain generation.
 * <p>
 * THIS SHOULD ONLY EVER BE USED ONCE PER CUBE.
 */
//TODO: make it also update blocklight
public class FirstLightProcessor {
	private static final IntHash.Strategy CUBE_Y_HASH = new IntHash.Strategy() {
		@Override public int hashCode(int e) {
			return e;
		}

		@Override public boolean equals(int a, int b) {
			return a == b;
		}
	};
	//mutableBlockPos variable to avoid creating thousands of instances of BlockPos
	private BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
	private ICubeCache cache;

	public FirstLightProcessor(ICubicWorld world) {
		this.cache = world.getCubeCache();
	}

	/**
	 * Sets blocks exposed to sunlight to full brightness.
	 *
	 * This method should NOT set blocks to dark.
	 *
	 * This method should be called as soon as a cube is filled with blocks. It updates only that one cube.
	 */
	public void earlySkylightMap(Cube cube) {
		Column column = cube.getColumn();

		CubeCoords cubePos = cube.getCoords();
		cubePos.forEachBlockPosMutableTopDown((pos)->{
			int topY = getHeightmapValue(column, blockToLocal(pos.getX()), blockToLocal(pos.getZ()));
			if(pos.getY() <= topY) {
				return false;
			}
			cube.setLightFor(EnumSkyBlock.SKY, pos, 15);
			return true;
		});
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
		Int2ObjectMap<FastCubeBlockAccess> blockAccess =
				new Int2ObjectOpenCustomHashMap<>(10, 0.75f, CUBE_Y_HASH);

		Column column = generatedCube.getColumn();
		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
				pos.setPos(blockX, pos.getY(), blockZ);
				int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				if (minBlockY > maxBlockY) {//is update needed?
					continue;
				}

				Iterable<Cube> cubes = column.getLoadedCubes(blockToCube(maxBlockY), blockToCube(minBlockY));
				int topBlockY = getHeightmapValue(column, blockToLocal(blockX), blockToLocal(blockZ));
				//going top-down
				for(Cube cube : cubes) {
					//generatedCube must be updated
					//the logic in canStopUpdating doesn't apply to it
					if(cube != generatedCube && canStopUpdating(cube, pos, topBlockY)) {
						break;
					}
					//if FirstLight isn't done in tha cube yet (and it's not the current cube) - t will be done later
					//so skip it there
					if(cube != generatedCube && !cube.isInitialLightingDone()) {
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

					if (!diffuseBlockColumnInCube(cube, minBlockY, maxBlockY, pos, topBlockY, blockAccess)) {
						throw new IllegalStateException("Check light failed at " + pos + "!");
					}
				}
			}
		}
		return true;
	}

	private boolean diffuseBlockColumnInCube(Cube cube, int minBlockY, int maxBlockY, BlockPos.MutableBlockPos pos, int topBlockY, Int2ObjectMap<FastCubeBlockAccess> blockAccessMap) {
		ICubicWorld world = cube.getWorld();
		int cubeMinBlockY = cubeToMinBlock(cube.getY());
		FastCubeBlockAccess blockAccess = blockAccessMap.get(cube.getY());
		if(blockAccess == null) {
			blockAccess = new FastCubeBlockAccess(cache, cube, 1);
			blockAccessMap.put(cube.getY(), blockAccess);
		}
		for (int y = 15; y >= 0; y--) {
			int blockY = y + cubeMinBlockY;

			if (blockY > maxBlockY || blockY < minBlockY) {
				continue;
			}
			pos.setY(blockY);
			if (!needsUpdate(blockAccess, pos, topBlockY)) {
				continue;
			}

			if (!world.checkLightFor(EnumSkyBlock.SKY, pos)) {
				return false;
			}
		}
		return true;
	}

	private boolean needsUpdate(FastCubeBlockAccess access, BlockPos.MutableBlockPos pos, int topBlockY) {
		//opaque blocks don't need update. Nothing can emit skylight, and skylight can't get into them nor out of them
		if (access.getBlockLightOpacity(pos) >= 15) {
			return false;
		}
		//this is the logic that world.checkLightFor uses to determine if it should update anything further
		//this is done here to avoid isAreaLoaded call (a lot of them quickly add up to a lot of time)
		//it first calculates light value as if the light here didn't exist, but all others remained
		//assuming non-opaque block - it's 1 less than neighbor with highest value
		//and then it checks all neighbors saved values (values stored in memory)
		//if savedValue-opacity matches the computed value from previous block - it will be updated
		//this isn't very straightforward logic but it works
		int computedLight = access.computeLightValue(pos);
		for (EnumFacing facing : EnumFacing.values()) {
			pos.move(facing);
			int currentLight = access.getLightFor(EnumSkyBlock.SKY, pos);
			int currentOpacity = Math.max(1, access.getBlockLightOpacity(pos));
			pos.move(facing.getOpposite());

			if (computedLight == currentLight - currentOpacity) {
				return true;
			}
		}
		return false;
	}

	private boolean canStopUpdating(Cube cube, BlockPos.MutableBlockPos pos, int topBlockY) {
		//NOTE: The logic here doesn't apply to the main cube currently being updated. Only to cubes below it.
		pos.setY(cube.getCoords().getMaxBlockY());
		boolean isDirectSkylight = pos.getY() > topBlockY;
		int lightValue = cube.getLightFor(EnumSkyBlock.SKY, pos);
		//if the top of the cube has no direct sunlight and the light value doesn't need update
		//then all blocks below also don't need update (as you go down skylight can only decrease,
		//and with worldgen light updates only whole block columns are updated at a time
		//in other cases update will be queued or done on load time
		//when there is no direct skylight, update is needed only
		//if the light value is 15 - exposed to direct skylight

		//the same logic can't be applied in case there is direct skylight -
		//at the top of the first cube the light value is correct
		return !isDirectSkylight && lightValue < 15;
	}

	private Pair<Integer, Integer> getMinMaxLightUpdateY(ICubeCache cache, Cube cube, int blockX, int blockZ) {
		//getHeightmapValue(x, y, z) = heightOfTopBlock+1
		int heightMax = getHeightmapValue(cache, blockX, blockZ);//==Y of the top block

		int cubeY = cube.getY();
		//if the top block is below current cube - Everything is fully lit, no update needed
		if (blockToCube(heightMax) < cubeY) {
			return null;
		}
		if (cubeY < blockToCube(heightMax)) {
			//this cube is below the top block
			//this means that eighter this cube didn't cause any updates by itself so inly that cube by itself needs to be updated
			//or that something has been generated above it before FirstLightProcessor diffuse but it could affect something below
			//For the cube above FirstLightProcessor won't update anything below the first block found below that cube
			//so updates below current cube need to be handled now, down to next block below it
			int topBlockYInThisCubeOrBelow = getHeightmapBelowCubeY(cache, blockX, blockZ, cube.getY() + 1);
			int topBlockCubeYInThisCubeOrBelow = blockToCube(topBlockYInThisCubeOrBelow);
			if(topBlockCubeYInThisCubeOrBelow == cubeY) {
				//this cube has some block in this block column, fine the next block below that cube
				int heightBelowCube = getHeightmapBelowCubeY(cache, blockX, blockZ, cube.getY()) + 1;
				return new ImmutablePair<>(heightBelowCube,  cubeToMaxBlock(cubeY));
			}
			return new ImmutablePair<>(cubeToMinBlock(cubeY), cubeToMaxBlock(cubeY));
		}

		int heightBelowCube = getHeightmapBelowCubeY(cache, blockX, blockZ, cubeY);
		return new ImmutablePair<>(heightBelowCube, heightMax);
	}

	/**
	 * Returns Y coordinate of the top block at (blockX, blockZ)
	 */
	private int getHeightmapValue(ICubeCache cache, int blockX, int blockZ) {
		int cubeX = blockToCube(blockX);
		int cubeZ = blockToCube(blockZ);

		int localX = blockToLocal(blockX);
		int localZ = blockToLocal(blockZ);

		Column column = cache.getColumn(cubeX, cubeZ);

		return getHeightmapValue(column, localX, localZ);
	}

	/**
	 * Returns Y coordinate of the top block at (localX, localZ) in column.
	 */
	private int getHeightmapValue(Column column, int localX, int localZ) {
		Integer val = column.getOpacityIndex().getTopBlockY(localX, localZ);
		return val == null ? Integer.MIN_VALUE/2 : val;
	}

	/**
	 * Returns Y coordinate of the top block below cubeY at (blockX, blockZ)
	 */
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
}
