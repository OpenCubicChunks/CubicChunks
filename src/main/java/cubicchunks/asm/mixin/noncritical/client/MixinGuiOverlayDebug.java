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

import cubicchunks.world.CubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Goal of this mixin is to remove the "Outside of world..." message on the debug overlay for cubic world types.
 * We redirect the call to BlockPos.getY() for the bounds check in GuiOverlayDebug.call only
 *
 * @author Malte Schütze
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {

    @Shadow @Final private Minecraft mc;

    @Group(name = "getMinWorldHeight", min = 1, max = 1)
    @ModifyConstant(
            method = "call",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO),
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/multiplayer/WorldClient;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;isEmpty()Z")
            ))
    private int getMinWorldHeight(int orig) {
        return ((CubicWorld) mc.world).getMinHeight();
    }

    // slice not exactly necessary here, but this is a long method that could change, so keep the slice
    @Group(name = "getMaxWorldHeight", min = 1, max = 1)
    @ModifyConstant(
            method = "call",
            constant = @Constant(intValue = 256),
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/multiplayer/WorldClient;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;isEmpty()Z")
            ))
    private int getMaxWorldHeight(int orig) {
        return ((CubicWorld) mc.world).getMaxHeight();
    }
}
