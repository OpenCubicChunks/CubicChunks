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

import cubicchunks.asm.MixinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;
import static cubicchunks.asm.JvmNames.GUI_OVERLAY_DEBUG_CALL;

/**
 * Goal of this mixin is to remove the "Outside of world..." message on the debug overlay for cubic world types.
 * We redirect the call to BlockPos.getY() for the bounds check in GuiOverlayDebug.call only
 * @author Malte Schütze
 */
@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {
	@Shadow @Final private Minecraft mc;

	// As of Minecraft 1.10.2 / Forge 12.18.1.2092, the bounds checks are call 5 and 6 to getY()

	// This is call 5 (idx 4)
	@Redirect(method = GUI_OVERLAY_DEBUG_CALL, at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 4))
	private int blockPosGetYBoundsCheck1(BlockPos pos) {
		return MixinUtils.getReplacementY(mc.theWorld, pos);

	}
	// This is call 6 (idx 5)
	@Redirect(method = GUI_OVERLAY_DEBUG_CALL, at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 5))
	private int blockPosGetYBoundsCheck2(BlockPos pos) {
		return MixinUtils.getReplacementY(mc.theWorld, pos);
	}
}
