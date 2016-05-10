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
package cubicchunks.asm.mixin.core;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Modifies ChunkCache to support extended world height.
 * <p>
 * ChunkCache is used by some AI code and (as subclass of ChunkCache) - block rendering code.
 * getBlockState is used only in AI code.
 */
@Mixin(ChunkCache.class)
public class MixinChunkCache_HeightLimits {

	@Shadow protected int chunkX;

	@Shadow protected int chunkZ;

	@Shadow protected Chunk[][] chunkArray;

	@Shadow protected World worldObj;

	private ICubicWorld world() {
		return (ICubicWorld) worldObj;
	}

	/**
	 * Overwrite to support extended world height.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public IBlockState getBlockState(BlockPos pos) {
		int minBlockY = world().getMinHeight();
		int maxBlockY = world().getMaxHeight();
		if (pos.getY() < minBlockY || pos.getY() >= maxBlockY) {
			return Blocks.AIR.getDefaultState();
		}
		int localChunkX = Coords.blockToCube(pos.getX()) - this.chunkX;
		int localChunkZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;

		if (localChunkX < 0 || localChunkX >= this.chunkArray.length ||
				localChunkZ < 0 || localChunkZ >= this.chunkArray[localChunkX].length) {
			return Blocks.AIR.getDefaultState();
		}
		Chunk chunk = this.chunkArray[localChunkX][localChunkZ];

		if (chunk != null) {
			return chunk.getBlockState(pos);
		}
		return Blocks.AIR.getDefaultState();
	}
}
