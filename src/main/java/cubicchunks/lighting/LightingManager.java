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

import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import cubicchunks.util.Coords;
import cubicchunks.world.ClientHeightMap;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.localToBlock;

//TODO: extract interfaces when it's done
public class LightingManager {

	private static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
	private ICubicWorld world;

	public LightingManager(ICubicWorld world) {
		this.world = world;
	}

	public CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
		return new CubeLightUpdateInfo(cube);
	}

	//TODO: make it private
	public void columnSkylightUpdate(UpdateType type, Column column, int localX, int minY, int maxY, int localZ) {
		int blockX = Coords.localToBlock(column.getX(), localX);
		int blockZ = Coords.localToBlock(column.getZ(), localZ);
		switch (type) {
			case IMMEDIATE:
				IntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
				for (IntCursor cubeY : toDiffuse) {
					boolean success = updateDiffuseLight(column.getCube(cubeY.value), localX, localZ, minY, maxY);
					if (!success) {
						markCubeBlockColumnForUpdate(column.getLoadedCube(cubeY.value), blockX, blockZ);
					}
				}
				break;
			case QUEUED:
				toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
				for (IntCursor cubeY : toDiffuse) {
					markCubeBlockColumnForUpdate(column.getLoadedCube(cubeY.value), blockX, blockZ);
				}
				break;
		}
	}

	private boolean updateDiffuseLight(Cube cube, int localX, int localZ, int minY, int maxY) {
		ICubicWorld world = cube.getCubicWorld();
		int minCubeY = cube.getCoords().getMinBlockY();
		int maxCubeY = cube.getCoords().getMaxBlockY();

		int blockX = localToBlock(cube.getX(), localX);
		int blockZ = localToBlock(cube.getZ(), localZ);

		for (int blockY = minCubeY; blockY <= maxCubeY; blockY++) {
			if (blockY < minY || blockY > maxY) {
				continue;
			}
			if (!world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(blockX, blockY, blockZ))) {
				return false;
			}
		}
		return true;
	}

	public void doOnBlockSetLightUpdates(Column column, BlockPos changePos, IBlockState newBlockState, int oldOpacity) {
		int newOpacity = newBlockState.getLightOpacity(column.getWorld(), changePos);
		if (!needsUpdate(oldOpacity, newOpacity)) {
			//nothing to update, this will frequently happen in ore generation
			return;
		}

		int localX = Coords.blockToLocal(changePos.getX());
		int localZ = Coords.blockToLocal(changePos.getZ());

		IHeightMap heightMap = column.getOpacityIndex();

		// did the top non-transparent block change?
		int oldTopY = heightMap.getTopBlockY(localX, localZ);
		heightMap.onOpacityChange(localX, changePos.getY(), localZ, newOpacity);
		column.setModified(true);

		int newTopY = findNewTopBlockY(heightMap, changePos, column, newOpacity, localX, localZ, oldTopY);

		int minY = Math.min(oldTopY, newTopY);
		int maxY = Math.max(oldTopY, newTopY);
		assert minY <= maxY;

		this.columnSkylightUpdate(UpdateType.IMMEDIATE, column, localX, minY, maxY, localZ);
	}

	//TODO: make it private
	public void markCubeBlockColumnForUpdate(Cube cube, int blockX, int blockZ) {
		CubeLightUpdateInfo data = cube.getCubeLightUpdateInfo();
		data.markBlockColumnForUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
	}

	private int findNewTopBlockY(IHeightMap heightMap, BlockPos changePos, Column column, int newOpacity, int localX, int localZ, int oldTopY) {
		if (!column.getWorld().isRemote) {
			return heightMap.getTopBlockY(localX, localZ);
		}
		//to avoid unnecessary delay when breaking blocks client needs to figure out new height before
		//server tells the client what it is
		//common cases first
		if (addedTopBlock(changePos, newOpacity, oldTopY)) {
			//added new block, so it's correct. Server update will be ignored
			return changePos.getY();
		}
		if (!changedTopToTransparent(changePos, newOpacity, oldTopY)) {
			//if not breaking the top block - no changes
			return oldTopY;
		}
		assert !(newOpacity == 0 && oldTopY < changePos.getY()) : "Changed transparent block into transparent!";

		//changed the top block
		int newTop = oldTopY - 1;
		while (column.getBlockLightOpacity(new BlockPos(localX, newTop, localZ)) == 0 && newTop > oldTopY - MAX_CLIENT_LIGHT_SCAN_DEPTH) {
			newTop--;
		}
		//update the heightmap. If this update it not accurate - it will be corrected when server sends block update
		((ClientHeightMap) heightMap).setHeight(localX, localZ, newTop);
		return newTop;
	}

	private boolean changedTopToTransparent(BlockPos changePos, int newOpacity, int oldTopY) {
		return newOpacity == 0 && changePos.getY() == oldTopY;
	}

	private boolean addedTopBlock(BlockPos pos, int newOpacity, int oldTopY) {
		return (pos.getY() > oldTopY) && newOpacity != 0;
	}

	private boolean needsUpdate(int oldOpacity, int newOpacity) {
		//update is needed only if opacities are different and booth are less than 15
		return oldOpacity != newOpacity && (oldOpacity < 15 || newOpacity < 15);
	}

	public void onHeightMapUpdate(Column column, int localX, int localZ, int oldHeight, int newHeight) {
		int minCubeY = blockToCube(Math.min(oldHeight, newHeight));
		int maxCubeY = blockToCube(Math.max(oldHeight, newHeight));
		for (Cube cube : column.getLoadedCubes()) {
			if (cube.getY() >= minCubeY && cube.getY() <= maxCubeY) {
				markCubeBlockColumnForUpdate(cube, localX, localZ);
			}
		}
	}

	public enum UpdateType {
		IMMEDIATE, QUEUED
	}

	//this will be interface
	public static class CubeLightUpdateInfo {
		private final Cube cube;
		private final boolean[] toUpdateColumns = new boolean[Cube.SIZE*Cube.SIZE];
		private boolean hasUpdates;

		public CubeLightUpdateInfo(Cube cube) {
			this.cube = cube;
		}

		public void markBlockColumnForUpdate(int localX, int localZ) {
			toUpdateColumns[index(localX, localZ)] = true;
			hasUpdates = true;
		}

		public void tick() {
			if (!this.hasUpdates) {
				return;
			}
			for (int localX = 0; localX < Cube.SIZE; localX++) {
				for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
					if (!toUpdateColumns[index(localX, localZ)]) {
						continue;
					}
					int height = cube.getColumn().getOpacityIndex().getTopBlockY(localX, localZ);

					for (int localY = 0; localY < Cube.SIZE; localY++) {
						int skylight = cube.getSkylight(localX, localY, localZ);
						int blockY = localToBlock(cube.getY(), localY);
						boolean isAbove = blockY > height;
						//unlike in FirstLightProcessor - don't need to care about cubes above possibly
						//affecting this -they are updated separately
						if ((isAbove && skylight < 15) || (!isAbove && skylight == 15)) {
							int blockX = localToBlock(cube.getX(), localX);
							int blockZ = localToBlock(cube.getZ(), localZ);
							BlockPos updatePos = new BlockPos(blockX, blockY, blockZ);
							if (!cube.getCubicWorld().checkLightFor(EnumSkyBlock.SKY, updatePos)) {
								//don't waste more time processing updates in cube at the edge
								return;
							}
						}
					}
					toUpdateColumns[index(localX, localZ)] = false;
				}
			}
			this.hasUpdates = false;
		}

		private int index(int x, int z) {
			return x << 4 | z;
		}
	}
}
