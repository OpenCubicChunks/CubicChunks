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
package cubicchunks.asm.mixin;

import cubicchunks.asm.AsmWorldHooks;
import cubicchunks.util.MathUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static cubicchunks.asm.AsmWorldHooks.getMaxHeight;
import static cubicchunks.asm.AsmWorldHooks.getMinHeight;

@Mixin(World.class)
public abstract class MixinWorld {

	@Shadow private int skylightSubtracted;

	@Shadow public abstract Chunk getChunkFromBlockCoords(BlockPos pos);

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	@Overwrite
	public boolean isValid(BlockPos pos) {
		if (pos.getX() < -30000000 || pos.getX() > 30000000) {
			return false;
		}
		if (pos.getY() < getMinHeight((World) (Object) this) || pos.getY() >= AsmWorldHooks.getMaxHeight((World) (Object) this)) {
			return false;
		}
		if (pos.getZ() < -30000000 || pos.getZ() > 30000000) {
			return false;
		}
		return true;
	}

	@Overwrite
	public int getLight(BlockPos pos) {
		if (pos.getY() < getMinHeight((World) (Object) this)) {
			return 0;
		}
		if (pos.getY() > getMaxHeight((World) (Object) this)) {
			return 15;
		}
		return this.getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
	}

	@Overwrite
	public int getLight(BlockPos pos, boolean checkNeighbors) {
		if (!this.isValid(pos)) {
			if (pos.getY() < getMinHeight((World) (Object) this)) {
				return 0;
			}
			return 15;
		}
		if (checkNeighbors && this.getBlockState(pos).useNeighborBrightness()) {
			int up = this.getLight(pos.up(), false);
			int east = this.getLight(pos.east(), false);
			int west = this.getLight(pos.west(), false);
			int south = this.getLight(pos.south(), false);
			int north = this.getLight(pos.north(), false);
			return MathUtil.max(up, east, west, south, north);
		}
		Chunk chunk = this.getChunkFromBlockCoords(pos);
		return chunk.getLightSubtracted(pos, this.skylightSubtracted);
	}
}
