/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.vertviewdist;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = RenderGlobal.class, priority = 2000)
public abstract class MixinRenderGlobal {
    @Shadow
    private ViewFrustum viewFrustum;
    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private int renderDistanceChunks;

    @Shadow
    public abstract void loadRenderers();

    @Shadow
    private WorldClient world;
    // NOTE: the field name here and in MixinRendeerGlobalNoOptifine have to match
    private int verticalRenderDistanceCubes;

    @Inject(
            method = "loadRenderers",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderDistanceChunks:I"
            )
    )
    private void onUpdateRenderDistance(CallbackInfo cbi) {
        this.verticalRenderDistanceCubes = CubicChunksConfig.verticalCubeLoadDistance;
    }

    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void onSetupTerrain(CallbackInfo cbi) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        // check for equal render distance to avoid calling loadRenderers twice
        if (mc.gameSettings.renderDistanceChunks == renderDistanceChunks
                && CubicChunksConfig.verticalCubeLoadDistance != verticalRenderDistanceCubes) {
            this.loadRenderers();
        }
    }

    @Redirect(
            method = "setupTerrain",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderDistanceChunks:I"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V")
            )
    )
    private int onGetRenderDistance(RenderGlobal _this) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return mc.gameSettings.renderDistanceChunks;
        }
        return Math.max(mc.gameSettings.renderDistanceChunks, CubicChunksConfig.verticalCubeLoadDistance);
    }
}
