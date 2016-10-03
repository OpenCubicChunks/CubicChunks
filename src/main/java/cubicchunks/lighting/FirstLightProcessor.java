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

import static net.minecraft.util.math.BlockPos.MutableBlockPos;

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

	private static final int LIGHT_UPDATE_RADIUS = 17;

	private static final int CUBE_RADIUS = Cube.SIZE / 2;

	private static final int UPDATE_BUFFER_RADIUS = 1;

	private static final int UPDATE_RADIUS = LIGHT_UPDATE_RADIUS + CUBE_RADIUS + UPDATE_BUFFER_RADIUS;

	private static final int DEFAULT_OCCLUSION_HEIGHT = Integer.MIN_VALUE / 2;

	private static final IntHash.Strategy CUBE_Y_HASH = new IntHash.Strategy() {

		@Override
		public int hashCode(int e) {
			return e;
		}

		@Override
		public boolean equals(int a, int b) {
			return a == b;
		}
	};


	private final MutableBlockPos mutablePos = new MutableBlockPos();

	private final ICubeCache cache;


	/**
	 * Determines if the block at the given position requires a skylight update.
	 *
	 * @param access a FastCubeBlockAccess providing access to the block
	 * @param pos the block's global position
	 * @return true if the specified block needs a skylight update, false otherwise
	 */
	private static boolean needsSkylightUpdate(FastCubeBlockAccess access, MutableBlockPos pos) {

		// Opaque blocks don't need update. Nothing can emit skylight, and skylight can't get into them nor out of them.
		if (access.getBlockLightOpacity(pos) >= 15) {
			return false;
		}

		// This is the logic that world.checkLightFor uses to determine if it should continue updating.
		// This is done here to avoid isAreaLoaded call (a lot of them quickly add up to a lot of time).
		// It first calculates the expected skylight value of this block and then it checks the neighbors' saved values,
		// if the saved value matches the expected value, it will be updated.
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

	/**
	 * Determines if light in the given cube can be updated.
	 *
	 * @param cube the cube whose light is supposed to be updated
	 * @return true if light in the given cube can be updated, false otherwise
	 */
	private static boolean canUpdateCube(Cube cube) {
		BlockPos cubeCenter = Coords.getCubeCenter(cube);
		return cube.getWorld().testForCubes(cubeCenter, UPDATE_RADIUS, c -> true);
	}

	/**
	 * Returns the y-coordinate of the highest occluding block in the specified block column. If there exists no such
	 * block {@link #DEFAULT_OCCLUSION_HEIGHT} will be returned instead.
	 *
	 * @param column the column containing the block column
	 * @param localX the block column's local x-coordinate
	 * @param localZ the block column's local z-coordinate
	 * @return the y-coordinate of the highest occluding block in the specified block column or
	 *         {@link #DEFAULT_OCCLUSION_HEIGHT} if no such block exists
	 */
	private static int getOcclusionHeight(Column column, int localX, int localZ) {
		Integer val = column.getOpacityIndex().getTopBlockY(localX, localZ);
		return val == null ? DEFAULT_OCCLUSION_HEIGHT : val;
	}

	/**
	 * Returns the y-coordinate of the highest occluding block in the specified block column, that is underneath the
	 * cube at the given y-coordinate. If there exists no such block {@link #DEFAULT_OCCLUSION_HEIGHT} will be returned
	 * instead.
	 *
	 * @param column the column containing the block column
	 * @param blockX the block column's global x-coordinate
	 * @param blockZ the block column's global z-coordinate
	 * @param cubeY the y-coordinate of the cube underneath which the highest occluding block is to be found
	 * @return the y-coordinate of the highest occluding block underneath the given cube in the specified block column
	 *         or {@link #DEFAULT_OCCLUSION_HEIGHT} if no such block exists
	 */
	private static int getOcclusionHeightBelowCubeY(Column column, int blockX, int blockZ, int cubeY) {
		IOpacityIndex index = column.getOpacityIndex();
		Integer val = index.getTopBlockYBelow(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ), Coords.cubeToMinBlock(cubeY));
		return val == null ? Integer.MIN_VALUE/2 : val;
	}

	/**
	 * Determines which vertical section of the specified block column in the given cube requires a lighting update
	 * based on the current occlusion in the cube's column.
	 *
	 * @param cube the cube inside of which the skylight is to be updated
	 * @param localX the local x-coordinate of the block column
	 * @param localZ the local z-coordinate of the block column
	 * @return a pair containing the minimum and the maximum y-coordinate to be updated in the given cube
	 */
	private static ImmutablePair<Integer, Integer> getMinMaxLightUpdateY(Cube cube, int localX, int localZ) {

		Column column = cube.getColumn();
		int heightMax = getOcclusionHeight(column, localX, localZ);//==Y of the top block

		// If the given cube is above the highest occluding block in the column, everything is fully lit.
		int cubeY = cube.getY();
		if (Coords.blockToCube(heightMax) < cubeY) {
			return null;
		}

		int blockX = Coords.cubeToMinBlock(cube.getX()) + localX;
		int blockZ = Coords.cubeToMinBlock(cube.getZ()) + localZ;

		// If the given cube lies underneath the occluding block, it must be updated from the top down.
		if (cubeY < Coords.blockToCube(heightMax)) {

			// Determine the y-coordinate of the highest block (and its cube) occluding blocks inside of the given cube
			// or further down.
			int topBlockYInThisCubeOrBelow = getOcclusionHeightBelowCubeY(column, blockX, blockZ, cube.getY() + 1);
			int topBlockCubeYInThisCubeOrBelow = Coords.blockToCube(topBlockYInThisCubeOrBelow);

			// If the given cube contains the occluding block, the update can be limited down to that block.
			if(topBlockCubeYInThisCubeOrBelow == cubeY) {
				int heightBelowCube = getOcclusionHeightBelowCubeY(column, blockX, blockZ, cube.getY()) + 1;
				return new ImmutablePair<>(heightBelowCube,  Coords.cubeToMaxBlock(cubeY));
			}
			// Otherwise, the whole height of the cube must be updated.
			else {
				return new ImmutablePair<>(Coords.cubeToMinBlock(cubeY), Coords.cubeToMaxBlock(cubeY));
			}
		}

		// ... otherwise, the update must start at the occluding block.
		int heightBelowCube = getOcclusionHeightBelowCubeY(column, blockX, blockZ, cubeY);
		return new ImmutablePair<>(heightBelowCube, heightMax);
	}


	/**
	 * Creates a new FirstLightProcessor for the given world.
	 *
	 * @param world the world for which the FirstLightProcessor will be used
	 */
	public FirstLightProcessor(ICubicWorld world) {
		this.cache = world.getCubeCache();
	}


	/**
	 * Initializes skylight in the given cube. The skylight will be consistent with respect to the world configuration
	 * and already existing cubes. It is however possible for cubes being considered lit at this stage to be occluded
	 * by cubes being generated further up.
	 *
	 * @param cube the cube whose skylight is to be initialized
	 */
	public void initializeSkylight(Cube cube) {
		IOpacityIndex opacityIndex = cube.getColumn().getOpacityIndex();

		int cubeMinY = Coords.cubeToMinBlock(cube.getY());

		for (int localX = 0; localX < Cube.SIZE - 1; ++localX) {
			for (int localZ = 0; localZ < Cube.SIZE - 1; ++localZ) {
				for (int localY = Cube.SIZE - 1; localY >= 0; --localY) {

					if (opacityIndex.isOccluded(localX, cubeMinY + localY, localZ)) {
						break;
					}

					cube.setSkylight(localX, localY, localZ, 15);
				}
			}
		}
	}

	/**
	 * Diffuses skylight in the given cube and all cubes affected by this update.
	 *
	 * @param cube the cube whose skylight is to be initialized
	 */
	public void diffuseSkylight(Cube cube) {
		ICubicWorld world = cube.getWorld();

		// Cache min/max Y, generating them may be expensive
		int[][] minBlockYArr = new int[16][16];
		int[][] maxBlockYArr = new int[16][16];

		int minBlockX = Coords.cubeToMinBlock(cube.getX());
		int maxBlockX = Coords.cubeToMaxBlock(cube.getX());

		int minBlockZ = Coords.cubeToMinBlock(cube.getZ());
		int maxBlockZ = Coords.cubeToMaxBlock(cube.getZ());

		// Determine the block columns that require updating. If there is nothing to update, store contradicting data so
		// we can skip the column later.
		for (int localX = 0; localX <= Cube.SIZE - 1; ++localX) {
			for (int localZ = 0; localZ <= Cube.SIZE - 1; ++localZ) {
				Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cube, localX, localZ);
				minBlockYArr[localX][localZ] = minMax == null ? Integer.MAX_VALUE : minMax.getLeft();
				maxBlockYArr[localX][localZ] = minMax == null ? Integer.MIN_VALUE : minMax.getRight();
			}
		}

		Int2ObjectMap<FastCubeBlockAccess> blockAccessMap = new Int2ObjectOpenCustomHashMap<>(10, 0.75f, CUBE_Y_HASH);

		Column column = cube.getColumn();
		for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
			for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {

				this.mutablePos.setPos(blockX, this.mutablePos.getY(), blockZ);
				int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
				int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];

				// If no update is needed, skip the block column.
				if (minBlockY > maxBlockY) {
					continue;
				}

				// Iterate over all affected cubes.
				Iterable<Cube> cubes = column.getLoadedCubes(Coords.blockToCube(maxBlockY), Coords.blockToCube(minBlockY));
				for (Cube otherCube : cubes) {
					int cubeY = otherCube.getY();

					// Skip this cube if an update is not possible.
					if(!canUpdateCube(otherCube)) {
						int minScheduledY = Math.max(Coords.cubeToMinBlock(cubeY), minBlockY);
						int maxScheduledY = Math.min(Coords.cubeToMaxBlock(cubeY), maxBlockY);

						// Queue the update to be processed once the cube is ready for it.
						world.getLightingManager().queueDiffuseUpdate(otherCube, this.mutablePos.getX(), this.mutablePos.getZ(), minScheduledY, maxScheduledY);
						continue;
					}

					// Update the block column in this cube.
					if (!diffuseSkylightInBlockColumn(otherCube, this.mutablePos, minBlockY, maxBlockY, blockAccessMap)) {
						throw new IllegalStateException("Check light failed at " + this.mutablePos + "!");
					}
				}
			}
		}
	}

	/**
	 * Diffuses skylight inside of the given cube in the block column specified by the given MutableBlockPos. The
	 * update is limited vertically by minBlockY and maxBlockY.
	 *
	 * @param cube the cube inside of which the skylight is to be diffused
	 * @param pos the xz-position of the block column to be updated
	 * @param minBlockY the lower bound of the section to be updated
	 * @param maxBlockY the upper bound of the section to be updated
	 * @return true if the update was successful, false otherwise
	 */
	private boolean diffuseSkylightInBlockColumn(Cube cube, MutableBlockPos pos, int minBlockY, int maxBlockY, Int2ObjectMap<FastCubeBlockAccess> blockAccessMap) {
		ICubicWorld world = cube.getWorld();

		int cubeMinBlockY = Coords.cubeToMinBlock(cube.getY());
		int cubeMaxBlockY = Coords.cubeToMaxBlock(cube.getY());

		int blockYMax = Math.min(cubeMaxBlockY, maxBlockY);
		int blockYMin = Math.max(cubeMinBlockY, minBlockY);

		FastCubeBlockAccess blockAccess = blockAccessMap.get(cube.getY());
		if (blockAccess == null) {
			blockAccess = new FastCubeBlockAccess(this.cache, cube, UPDATE_BUFFER_RADIUS);
			blockAccessMap.put(cube.getY(), blockAccess);
		}

		for (int blockY = blockYMax; blockY >= blockYMin; --blockY) {

			pos.setY(blockY);
			if (!needsSkylightUpdate(blockAccess, pos)) {
				continue;
			}

			if (!world.checkLightFor(EnumSkyBlock.SKY, pos)) {
				return false;
			}
		}

		return true;
	}

}
