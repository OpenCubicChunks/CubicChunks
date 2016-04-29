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
package cubicchunks.asm.mixin.core.client;

import cubicchunks.asm.AsmWorldHooks;
import cubicchunks.util.Coords;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCache_HeightLimits {

	@Shadow protected int chunkX;

	@Shadow protected int chunkZ;

	@Shadow protected Chunk[][] chunkArray;

	@Shadow protected World worldObj;

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	/**
	 * Overwrite to support extended world height.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public int getLightFor(EnumSkyBlock type, BlockPos pos) {
		int minBlockY = AsmWorldHooks.getMinHeight(worldObj);
		int maxBlockY = AsmWorldHooks.getMaxHeight(worldObj);
		if (pos.getY() < minBlockY || pos.getY() >= maxBlockY) {
			return type.defaultLightValue;
		}
		int localChunkX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int localChunkZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (localChunkX < 0 || localChunkX >= chunkArray.length ||
				localChunkZ < 0 || localChunkZ >= chunkArray[localChunkX].length) {
			return type.defaultLightValue;
		}
		return this.chunkArray[localChunkX][localChunkZ].getLightFor(type, pos);
	}

	/**
	 * Overwrite to support extended world height.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	private int getLightForExt(EnumSkyBlock type, BlockPos pos) {
		if (type == EnumSkyBlock.SKY && this.worldObj.provider.getHasNoSky()) {
			return 0;
		}
		int minBlockY = AsmWorldHooks.getMinHeight(worldObj);
		int maxBlockY = AsmWorldHooks.getMaxHeight(worldObj);
		if (pos.getY() < minBlockY || pos.getY() >= maxBlockY) {
			return type.defaultLightValue;
		}
		if (this.getBlockState(pos).useNeighborBrightness()) {
			int light = 0;

			for (EnumFacing facing : EnumFacing.values()) {
				int lightNeighbor = this.getLightFor(type, pos.offset(facing));

				light = Math.max(light, lightNeighbor);

				if (light >= 15) {
					return light;
				}
			}
			return light;
		}
		int localChunkX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int localChunkZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
		if (localChunkX < 0 || localChunkX >= chunkArray.length ||
				localChunkZ < 0 || localChunkZ >= chunkArray[localChunkX].length) {
			return type.defaultLightValue;
		}
		if (chunkArray[localChunkX][localChunkZ] == null) {
			return type.defaultLightValue;
		}
		return this.chunkArray[localChunkX][localChunkZ].getLightFor(type, pos);
	}
}
