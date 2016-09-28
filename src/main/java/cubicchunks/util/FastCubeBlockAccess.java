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
package cubicchunks.util;

import cubicchunks.world.IColumnProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import static cubicchunks.util.Coords.blockToLocal;

/**
 * Simple class that allows to quickly access blocks near specified cube without the overhead of getting these cubes.
 * <p>
 * Does not allow to set blocks, only get blocks, their opacity and get/set light values.
 */
public class FastCubeBlockAccess {
	private final Cube[][][] cache;
	private final int originX, originY, originZ;
	private final ICubicWorld world;

	public FastCubeBlockAccess(IColumnProvider cache, Cube cube, int radius) {
		int n = radius*2 + 1;
		this.world = cube.getWorld();
		this.cache = new Cube[n][n][n];
		this.originX = cube.getX() - radius;
		this.originY = cube.getY() - radius;
		this.originZ = cube.getZ() - radius;

		for (int relativeCubeX = -radius; relativeCubeX <= radius; relativeCubeX++) {
			for (int relativeCubeZ = -radius; relativeCubeZ <= radius; relativeCubeZ++) {
				Column column = cache.provideChunk(
						originX + relativeCubeX + radius,
						originZ + relativeCubeZ + radius);

				for (int relativeCubeY = -radius; relativeCubeY <= radius; relativeCubeY++) {
					this.cache[relativeCubeX + radius][relativeCubeY + radius][relativeCubeZ + radius] = 
							column.getCube(originY + relativeCubeY + radius);
				}
			}
		}
	}

	private Cube getCube(int blockX, int blockY, int blockZ) {
		int cubeX = Coords.blockToCube(blockX);
		int cubeY = Coords.blockToCube(blockY);
		int cubeZ = Coords.blockToCube(blockZ);

		Cube cube = this.cache[cubeX - originX][cubeY - originY][cubeZ - originZ];
		return cube;
	}

	public IBlockState getBlockState(BlockPos pos) {
		return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	public IBlockState getBlockState(int blockX, int blockY, int blockZ) {
		return this.getCube(blockX, blockY, blockZ).getBlockState(blockX, blockY, blockZ);
	}

	public int getBlockLightOpacity(BlockPos pos) {
		return this.getBlockState(pos.getX(), pos.getY(), pos.getZ()).getLightOpacity((World) world, pos);
	}

	public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		return this.getCube(pos.getX(), pos.getY(), pos.getZ()).getLightFor(lightType, pos);
	}

	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
		this.getCube(pos.getX(), pos.getY(), pos.getZ()).setLightFor(lightType, pos, val);
	}

	/**
	 * Faster version of world.getRawLight that works for skylight
	 */
	public int computeLightValue(BlockPos pos) {
		Cube cube = getCube(pos.getX(), pos.getY(), pos.getZ());
		Column column = cube.getColumn();
		int height = column.getHeightValue(blockToLocal(pos.getX()), blockToLocal(pos.getZ()));
		if (pos.getY() > height) {
			return 15;
		}
		
		IBlockState iblockstate = cube.getBlockState(pos);
		int lightSubtract = iblockstate.getLightOpacity((IBlockAccess) world, pos);

		if (lightSubtract < 1) {
			lightSubtract = 1;
		}

		if (lightSubtract >= 15) {
			return 0;
		}
		BlockPos.PooledMutableBlockPos currentPos = BlockPos.PooledMutableBlockPos.retain();
		int maxValue = 0;
		for (EnumFacing enumfacing : EnumFacing.values()) {
			currentPos.setPos(pos).move(enumfacing);
			int currentValue = this.getLightFor(EnumSkyBlock.SKY,currentPos) - lightSubtract;

			if (currentValue > maxValue) {
				maxValue = currentValue;
			}

			if (maxValue >= 14) {
				return maxValue;
			}
		}

		currentPos.release();
		return maxValue;
	}
}
