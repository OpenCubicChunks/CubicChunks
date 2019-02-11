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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinConfig;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Modify vertical render distance so that it's cube.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ViewFrustum.class)
public class MixinViewFrustum_VertViewDistance {

    @Shadow @Final protected World world;
    @Shadow @Final protected RenderGlobal renderGlobal;
    private int renderDistance = 16;

    //this one can fail, there is safe default
    @Inject(method = "setCountChunksXYZ", at = @At(value = "HEAD"))
    private void onSetCountChunks(int renderDistance, CallbackInfo cbi) {
        if (((ICubicWorld) world).isCubicWorld()) {
            this.renderDistance = (CubicChunksMixinConfig.BoolOptions.VERT_RENDER_DISTANCE.getValue() ?
                    CubicChunksConfig.verticalCubeLoadDistance : renderDistance) * 2 + 1;
        } else {
            this.renderDistance = 16;
        }
    }

    @ModifyConstant(method = "setCountChunksXYZ", constant = @Constant(intValue = 16))
    private int getYViewDistance(int oldDistance) {
        return renderDistance;
    }
}
