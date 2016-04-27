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
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static cubicchunks.asm.AsmWorldHooks.getMaxHeight;
import static cubicchunks.asm.AsmWorldHooks.getMinHeight;

@Mixin(World.class)
public abstract class MixinWorld_HeightLimits {

	@Shadow private int skylightSubtracted;

	@Shadow public WorldProvider provider;

	@Shadow public abstract Chunk getChunkFromBlockCoords(BlockPos pos);

	@Shadow public abstract IBlockState getBlockState(BlockPos pos);

	@Shadow public abstract boolean isBlockLoaded(BlockPos pos);

	/**
	 * This @Overwrite allows World to "see" blocks outside of 0..255 height range.
	 * Currently redirecting constant loading is not supported.
	 *
	 * @author Barteks2x
	 */
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

	/**
	 * Replace getLight() with method that returns non-default light values
	 * outside of 0..255 height range.
	 * <p>
	 * Used in parts of game logic and entity rendering code.
	 * Doesn't directly affect block rendering.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public int getLight(BlockPos pos) {
		int minY = getMinHeight((World) (Object) this);
		if (pos.getY() < minY) {
			pos = new BlockPos(pos.getX(), minY, pos.getZ());
		}
		if (pos.getY() >= getMaxHeight((World) (Object) this)) {
			return EnumSkyBlock.SKY.defaultLightValue;
		}
		return this.getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
	}

	/**
	 * Replace getLight() with method that returns non-default light values
	 * outside of 0..255 height range.
	 * <p>
	 * Used in parts of game logic and entity rendering code.
	 * Doesn't directly affect block rendering.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public int getLight(BlockPos pos, boolean checkNeighbors) {
		if (!this.isValid(pos)) {
			int minY = getMinHeight((World) (Object) this);
			if (pos.getY() < minY) {
				pos = new BlockPos(pos.getX(), minY, pos.getZ());
			} else {
				return 15;
			}
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

	/**
	 * Replace getLightFor() with method that returns non-default light values
	 * outside of 0..255 height range.
	 * <p>
	 * Used in parts of game logic, entity rendering code and indirectly in block rendering.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public int getLightFor(EnumSkyBlock type, BlockPos pos) {
		int minY = getMinHeight((World) (Object) this);
		if (pos.getY() < minY) {
			pos = new BlockPos(pos.getX(), minY, pos.getZ());
		}
		if (!this.isValid(pos)) {
			return type.defaultLightValue;
		}
		if (!this.isBlockLoaded(pos)) {
			return type.defaultLightValue;
		}
		return this.getChunkFromBlockCoords(pos).getLightFor(type, pos);
	}

	/**
	 * Conditionally replaces isAreaLoaded with Cubic Chunks implementation
	 * (continues with vanilla code if it's not a cubic chunks world).
	 * World.isAreaLoaded is used to check if some things can be updated (like light).
	 * If it returns false - update doesn't happen. This fixes it
	 *
	 * NOTE: there are some methods that use it incorrectly
	 * ie. by checking it at some constant height (usually 0 or 64).
	 * These places need to be modified.
	 *
	 * @author Barteks2x
	 */
	@Group(name = "isLoaded", max = 1)
	@Inject(
			method = "isAreaLoaded(IIIIIIZ)Z",
			at = @At(value = "HEAD"),
			cancellable = true,
			require = 1
	)
	private void isAreaLoadedInject(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty, CallbackInfoReturnable cbi) {
		if (!AsmWorldHooks.isTallWorld((World) (Object) this)) {
			return;
		}
		boolean ret = AsmWorldHooks.isAreaLoaded(
				(World) (Object) this,
				xStart, yStart, zStart, xEnd, yEnd, zEnd, allowEmpty
		);
		cbi.cancel();
		cbi.setReturnValue(ret);
	}

	//TODO: modify isBlockLoaded to check for cubes. It currently breaks some parts of Minecraft because it's used with constant Y position.
}
