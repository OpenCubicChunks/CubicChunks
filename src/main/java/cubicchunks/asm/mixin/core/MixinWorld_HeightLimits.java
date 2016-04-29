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

import cubicchunks.asm.AsmWorldHooks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
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

	private World this_() {
		return (World) (Object) this;
	}

	/**
	 * This @Overwrite allows World to "see" blocks outside of 0..255 height range.
	 * Currently redirecting constant loading is not supported.
	 *
	 * @author Barteks2x
	 */
	@Overwrite
	public boolean isValid(BlockPos pos) {
		return pos.getX() >= -30000000 && pos.getX() <= 30000000 &&
				pos.getY() >= getMinHeight(this_()) && pos.getY() < getMaxHeight(this_()) &&
				pos.getZ() >= -30000000 && pos.getZ() <= 30000000;
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
		int minY = getMinHeight(this_());
		if (pos.getY() < minY) {
			return 0;
		}
		if (pos.getY() >= getMaxHeight((World) (Object) this)) {
			return EnumSkyBlock.SKY.defaultLightValue;
		}
		return this.getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
	}

	/**
	 * Redirect pos.getY() in getLight to return value in range 0..255
	 * when Y position is within cubic chunks height range, to workaround vanilla height check.
	 *
	 * this getLight method is used in parts of game logic and entity rendering code.
	 * Doesn't directly affect block rendering.
	 */
	@Group(name = "getLightHeightOverride", max = 3)
	@Redirect(
			method = "getLight(Lnet/minecraft/util/math/BlockPos;Z)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"),
			require = 2
	)
	private int onGetYGetLight(BlockPos pos) {
		if(pos.getY() < getMinHeight(this_()) || pos.getY() >= getMaxHeight(this_())) {
			return 64;//any value between 0 and 255
		}
		return pos.getY();
	}

	/**
	 * If max height is exceeded, the height is clamped to y=255,
	 * replace that 255 with actual world height
	 */
	@Group(name = "getLightHeightOverride")
	@ModifyConstant(
			method = "getLight(Lnet/minecraft/util/math/BlockPos;Z)I",
			constant = @Constant(intValue = 255),
			require = 1
	)
	private int getLightGetReplacementYForTooHighY(int original) {
		return getMaxHeight(this_()) - 1;
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
	 * <p>
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
