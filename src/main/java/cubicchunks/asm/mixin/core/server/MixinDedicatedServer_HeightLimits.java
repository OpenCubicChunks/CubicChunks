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
package cubicchunks.asm.mixin.core.server;

import cubicchunks.ICubicChunksWorldType;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.InetAddress;

import static cubicchunks.asm.JvmNames.DEDICATED_SERVER_IS_ANNOUNCING_PLAYER_ACHIEVEMENTS;

/**
 * Fix height limits in {@code DedicatedServer}
 */
@Mixin(DedicatedServer.class)
public class MixinDedicatedServer_HeightLimits {

	private WorldType worldtype;

	/**
	 * Get the worldType local variable
	 */
	@Inject(method = "startServer",
	        at = @At(value = "INVOKE", target = DEDICATED_SERVER_IS_ANNOUNCING_PLAYER_ACHIEVEMENTS),
	        locals = LocalCapture.CAPTURE_FAILHARD,
	        require = 1)
	private void getWorldTypeForBuildHeight(CallbackInfoReturnable<Boolean> cir, Thread thread, int i, InetAddress inetaddress, long j, String s, String s1, String s2, long k, WorldType worldtype) {
		this.worldtype = worldtype;
	}

	/**
	 * Replace the default build height (256).
	 */
	@ModifyConstant(method = "startServer", constant = @Constant(intValue = 256), require = 2)
	private int getDefaultBuildHeight(int oldValue) {
		if (worldtype instanceof ICubicChunksWorldType) {
			return ((ICubicChunksWorldType) worldtype).getMaximumPossibleHeight();
		}
		return oldValue;
	}
}
