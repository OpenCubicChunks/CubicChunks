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

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.asm.MixinUtils;
import cubicchunks.world.ICubicWorld;

import static cubicchunks.asm.JvmNames.BLOCK_POS;
import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;

@Mixin(World.class)
public abstract class MixinWorld_HeightLimits implements ICubicWorld {

	@Shadow public WorldProvider provider;

	@Shadow public abstract boolean isValid(BlockPos pos);

	@Shadow public abstract boolean isBlockLoaded(BlockPos pos);

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	@Shadow public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);

	@Shadow public abstract Chunk getChunkFromBlockCoords(BlockPos pos);

	/**
	 * Redirect BlockPos.getY() here to modify vanilla height check
	 */
	@Group(name = "getLightFromNeighborsFor", min = 2, max = 2)
	@Redirect(method = "getLightFromNeighborsFor", at = @At(value = "INVOKE", target = BLOCK_POS_GETY), require = 1)
	private int getLightFromNeighborsForBlockPosGetYRedirect(BlockPos pos) {
		return MixinUtils.getReplacementY(this, pos);
	}

	@Group(name = "getLightFromNeighborsFor")
	@ModifyArg(method = "getLightFromNeighborsFor",
	           at = @At(value = "INVOKE", target = BLOCK_POS + "<init>(III)V"),
	           index = 1,
	           require = 1)
	private int getLightFromNeighborsForGetMinHeight(int origY) {
		return this.getMinHeight();
	}
}
