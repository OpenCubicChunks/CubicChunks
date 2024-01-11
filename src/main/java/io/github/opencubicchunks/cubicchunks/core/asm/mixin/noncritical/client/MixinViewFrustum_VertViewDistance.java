/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinConfig;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
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
            ICubicWorld world = (ICubicWorld) this.world;
            // vanilla case: support extended height
            this.renderDistance = Coords.blockToCube(world.getMaxHeight()) - Coords.blockToCube(world.getMinHeight());
        }
    }

    @ModifyArg(method = "updateChunkPositions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition"
            + "(III)V"), index = 1)
    private int modifyRenderChunkPosWhenUpdatingPositions(int y) {
        // extended height support on vanilla worlds, no need to handle CC case - MixinViewFrustum_RenderHeightFix replaces this implementation
        return y + ((ICubicWorld) world).getMinHeight();
    }

    @ModifyVariable(method = "markBlocksForUpdate", at = @At("HEAD"), argsOnly = true, index = 2)
    private int modifyMinYForUpdate(int minY) {
        if (((ICubicWorld) world).isCubicWorld()) {
            return minY;
        }
        // vanilla case: support extended height
        return minY - ((ICubicWorld) world).getMinHeight();
    }

    @ModifyVariable(method = "markBlocksForUpdate", at = @At("HEAD"), argsOnly = true, index = 5)
    private int modifyMaxYForUpdate(int maxY) {
        if (((ICubicWorld) world).isCubicWorld()) {
            return maxY;
        }
        // vanilla case: support extended height
        return maxY - ((ICubicWorld) world).getMinHeight();
    }

    @Redirect(method = "getRenderChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"))
    private int modifyMaxYForUpdate(BlockPos instance) {
        if (((ICubicWorld) world).isCubicWorld()) {
            return instance.getY();
        }
        // vanilla case: support extended height
        return instance.getY() - ((ICubicWorld) world).getMinHeight();
    }

    @ModifyConstant(method = "setCountChunksXYZ", constant = @Constant(intValue = 16))
    private int getYViewDistance(int oldDistance) {
        return renderDistance;
    }
}
