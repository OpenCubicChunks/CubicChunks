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

package cubicchunks.asm.mixin.noncritical.client;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import static cubicchunks.asm.JvmNames.SYSTEM_NANOTIME;

/**
 * Fixes a bug that causes Cubes to render vary slow in 1.10
 * This also effects Vanilla, but lets fix it anyway as it becomes even more
 * of a problem do the the long distances that players can fall
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

	/**
	 * Replace the first System.nanoTime() call to cancel out the time spent
	 * before rendering (client world ticks and stuff)
	 * As far as I can tell this has no noticeable effect on fps (does not make more lag)
	 *
	 * CHECKED: 1.10.2-12.18.1.2092
	 */
	@Group(name = "renderLagFix")
	@Redirect(method = "updateCameraAndRender",
	          at = @At(value = "INVOKE", target = SYSTEM_NANOTIME, ordinal = 0))
	public long skipTime(float partialTicks, long nanoTime) {
		return nanoTime;
	}
}
