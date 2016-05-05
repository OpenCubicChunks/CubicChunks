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

import cubicchunks.util.MathUtil;
import cubicchunks.world.ICubicWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class MixinWorld_HeightLimits implements ICubicWorld {

	@Shadow public WorldProvider provider;

	@Shadow public abstract boolean isValid(BlockPos pos);

	@Shadow public abstract boolean isBlockLoaded(BlockPos pos);

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	@Shadow public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);

	@Shadow public abstract Chunk getChunkFromBlockCoords(BlockPos pos);

	/**
	 * Overwrite getLightFromNeighborsFor to use modified height limit.
	 */
	//TODO: Use @Redirect if possible when constant redirecting gets implemented.
	@Overwrite
	public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
		if (this.provider.getHasNoSky() && type == EnumSkyBlock.SKY) {
			return 0;
		}
		//changed 0 to minY
		int minY = this.getMinHeight();
		if (pos.getY() < minY) {
			pos = new BlockPos(pos.getX(), minY, pos.getZ());
		}

		if (!this.isValid(pos)) {
			return type.defaultLightValue;
		}
		if (!this.isBlockLoaded(pos)) {
			return type.defaultLightValue;
		}
		if (this.getBlockState(pos).useNeighborBrightness()) {
			int up = this.getLightFor(type, pos.up());
			int east = this.getLightFor(type, pos.east());
			int west = this.getLightFor(type, pos.west());
			int south = this.getLightFor(type, pos.south());
			int north = this.getLightFor(type, pos.north());
			return MathUtil.max(up, east, west, south, north);
		}
		return this.getChunkFromBlockCoords(pos).getLightFor(type, pos);
	}
}
