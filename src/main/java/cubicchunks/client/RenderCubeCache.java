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
package cubicchunks.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;

public class RenderCubeCache extends ChunkCache {
	protected int cubeY;
	protected Cube[][][] cubeArrays;
	private ICubicWorld world;

	public RenderCubeCache(ICubicWorld world, BlockPos from, BlockPos to, int subtract) {
		super((World) world, from, to, subtract);
		this.world = world;
		this.cubeY = Coords.blockToCube(from.getY() - subtract);
		int cubeXEnd = Coords.blockToCube(to.getX() + subtract);
		int cubeYEnd = Coords.blockToCube(to.getY() + subtract);
		int cubeZEnd = Coords.blockToCube(to.getZ() + subtract);

		cubeArrays = new Cube[cubeXEnd - this.chunkX + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.chunkZ + 1];

		for (int currentCubeX = chunkX; currentCubeX <= cubeXEnd; currentCubeX++) {
			for (int currentCubeY = cubeY; currentCubeY <= cubeYEnd; currentCubeY++) {
				for (int currentCubeZ = chunkZ; currentCubeZ <= cubeZEnd; currentCubeZ++) {
					cubeArrays[currentCubeX - chunkX][currentCubeY - cubeY][currentCubeZ -
						chunkZ] = world.getCubeFromCubeCoords(currentCubeX, currentCubeY, currentCubeZ);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public int getCombinedLight(BlockPos pos, int lightValue) {
		int blockLight = this.getLightForExt(EnumSkyBlock.SKY, pos);
		int skyLight = this.getLightForExt(EnumSkyBlock.BLOCK, pos);

		if (skyLight < lightValue) {
			skyLight = lightValue;
		}

		return blockLight << 20 | skyLight << 4;
	}

	@Override
	@Nullable public TileEntity getTileEntity(BlockPos pos) {
		int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
		int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
			arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
			arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
			return null;
		}
		if (this.cubeArrays[arrayX][arrayY][arrayZ] == null) {
			return null;
		}
		return this.cubeArrays[arrayX][arrayY][arrayZ].getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		if (pos.getY() < world.getMinHeight() | pos.getY() >= world.getMaxHeight()) {
			return Blocks.AIR.getDefaultState();
		}
		int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
		int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;

		if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
			arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
			arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
			return Blocks.AIR.getDefaultState();
		}
		Cube cube = this.cubeArrays[arrayX][arrayY][arrayZ];

		if (cube != null) {
			return cube.getBlockState(pos);
		}
		return Blocks.AIR.getDefaultState();
	}

	private int getLightForExt(EnumSkyBlock type, BlockPos pos) {
		if (type == EnumSkyBlock.SKY && this.world.getProvider().hasNoSky()) {
			return 0;
		}
		if (pos.getY() < world.getMinHeight() && pos.getY() >= world.getMaxHeight()) {
			return type.defaultLightValue;
		}
		if (this.getBlockState(pos).useNeighborBrightness()) {
			int max = 0;

			for (EnumFacing enumfacing : EnumFacing.values()) {
				int current = this.getLightFor(type, pos.offset(enumfacing));
				if (current > max) {
					max = current;
				}
				if (max >= 15) {
					return max;
				}
			}
			return max;
		}
		int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
		int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
			arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
			arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
			return type.defaultLightValue;
		}
		Cube cube = this.cubeArrays[arrayX][arrayY][arrayZ];
		if (cube == null) {
			return type.defaultLightValue;
		}
		return cube.getLightFor(type, pos);
	}

	@Override
	public int getLightFor(EnumSkyBlock type, BlockPos pos) {
		if (pos.getY() < world.getMinHeight() && pos.getY() >= world.getMaxHeight()) {
			return type.defaultLightValue;
		}
		int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
		int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
			arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
			arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
			return type.defaultLightValue;
		}
		Cube cube = this.cubeArrays[arrayX][arrayY][arrayZ];
		if (cube == null) {
			return type.defaultLightValue;
		}
		return cube.getLightFor(type, pos);
	}

	@Override
	public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
		//TODO: remove this comment when the bug is fixed in forge: CubicChunks: fix forge bug #3026
		if (pos.getY() < world.getMinHeight() || pos.getY() >= world.getMaxHeight()) {
			return defaultValue;
		}
		int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
		int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
			arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
			arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
			return defaultValue;
		}
		Cube cube = this.cubeArrays[arrayX][arrayY][arrayZ];
		if (cube == null) {
			return defaultValue;
		}
		IBlockState state = getBlockState(pos);
		return state.getBlock().isSideSolid(state, this, pos, side);
	}
}
