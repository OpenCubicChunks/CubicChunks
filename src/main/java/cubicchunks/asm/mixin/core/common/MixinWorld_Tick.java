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
package cubicchunks.asm.mixin.core.common;

import cubicchunks.world.ICubicWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static cubicchunks.asm.JvmNames.WORLD_GET_PERSISTENT_CHUNKS;
import static cubicchunks.asm.JvmNames.WORLD_IS_AREA_LOADED;

/**
 * World class mixins related to block and entity ticking.
 */
@Mixin(World.class)
public abstract class MixinWorld_Tick implements ICubicWorld {

	private int updateEntity_entityPosY;
	private int updateEntity_entityPosX;
	private int updateEntity_entityPosZ;

	@Shadow private boolean isValid(BlockPos pos) { throw new Error();}

	@Shadow public abstract boolean isAreaLoaded(int x1, int y1, int z1, int x2, int y2, int z2, boolean allowEmpty);

	/**
	 * Redirect {@code isAreaLoaded} here, to use Y coordinate of the entity.
	 * <p>
	 * Vanilla uses a constant Y because blocks below y=0 and above y=256 are never loaded, which means that entities would be getting stuck there.
	 */
	@Group(name = "updateEntity", max = 2, min = 2)
	@Redirect(method = "updateEntityWithOptionalForce",
	          at = @At(value = "INVOKE", target = WORLD_IS_AREA_LOADED),
	          require = 1)
	private boolean canUpdateEntity(World _this, int startBlockX, int oldStartBlockY, int startBlockZ, int endBlockX, int oldEndBlockY, int endBlockZ, boolean allowEmpty) {
		if (!this.isCubicWorld()) {
			return isAreaLoaded(startBlockX, oldStartBlockY, startBlockZ, endBlockX, oldEndBlockY, endBlockZ, allowEmpty);
		}
		return this.isRemote() ||
				this.getCubeFromBlockCoords(
						new BlockPos(updateEntity_entityPosX, updateEntity_entityPosY, updateEntity_entityPosZ))
						.getTickets().shouldTick();
	}

	/**
	 * Allows to get Y position of the updated entity.
	 */
	@Group(name = "updateEntity")
	@Inject(method = "updateEntityWithOptionalForce",
	        at = @At(value = "INVOKE", target = WORLD_GET_PERSISTENT_CHUNKS, remap = false),
	        locals = LocalCapture.CAPTURE_FAILHARD,
	        require = 1)
	public void onIsAreaLoadedForUpdateEntityWithOptionalForce(Entity entity, boolean force, CallbackInfo ci, int i, int j) {
		updateEntity_entityPosY = MathHelper.floor_double(entity.posY);
		updateEntity_entityPosX = MathHelper.floor_double(entity.posX);
		updateEntity_entityPosZ = MathHelper.floor_double(entity.posZ);
	}
}
