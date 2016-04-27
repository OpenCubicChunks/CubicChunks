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

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
/**
 * World class mixins related to block and entity ticking.
 */
@Mixin(World.class)
public abstract class MixinWorld_Tick {

	private int updateEntity_entityPosY;
	private int updateEntity_checkRadius;

	@Shadow public abstract  boolean isAreaLoaded(int startX, int startY, int startZ, int endX, int endY, int endZ, boolean allowEmpty);

	/**
	 * Replace isAreaLoaded startBlockY argument
	 */
	@Group(name = "updateEntity", max = 4)
	@ModifyArg(method = "updateEntityWithOptionalForce",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAreaLoaded(IIIIIIZ)Z"),
			index = 1/*startBlockY*/, require = 1)
	private int onEntityUpdateIsLoadedStartYPos(int oldStartBlockY) {
		return updateEntity_entityPosY - updateEntity_checkRadius;
	}

	/**
	 * Replace isAreaLoaded startBlockY argument
	 */
	@Group(name = "updateEntity")
	@ModifyArg(method = "updateEntityWithOptionalForce",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAreaLoaded(IIIIIIZ)Z"),
			index = 4/*startBlockY*/, require = 1)
	private int onEntityUpdateIsLoadedEndYPos(int oldStartBlockY) {
		return updateEntity_entityPosY + updateEntity_checkRadius;
	}

	/**
	 * This exists just to get the check radius, doesn't actually change the value.
	 */
	@Group(name = "updateEntity")
	@ModifyArg(
			method = "updateEntityWithOptionalForce",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAreaLoaded(IIIIIIZ)Z"),
			index = 0/*startBlockX*/, require = 1)
	private int onEntityUpdateIsLoadedStartXPos(int startBlockX) {
		//startBlockX == entityPosX - checkRadius <==> checkRadius == entityPosX - startBlockX
		this.updateEntity_checkRadius = updateEntity_entityPosY - startBlockX;
		return startBlockX;
	}

	/**
	 * Allows to get Y position of the updated entity.
	 */
	@Group(name = "updateEntity")
	@Inject(
			method = "updateEntityWithOptionalForce",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getPersistentChunks()" +
					"Lcom/google/common/collect/ImmutableSetMultimap;"),
			locals = LocalCapture.CAPTURE_FAILHARD, require = 1
	)
	public void onIsAreaLoadedForUpdateEntityWithOptionalForce(Entity entity, boolean force, CallbackInfo ci, int i, int j) {
		updateEntity_entityPosY = MathHelper.floor_double(entity.posY);
	}
}
