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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;
import java.util.Iterator;

@SuppressWarnings("target")
@Mixin({RenderList.class, VboRenderList.class})
public abstract class MixinRenderList extends ChunkRenderContainer {

    @Dynamic @Shadow(remap = false) private double viewEntityY;

    private int renderChunkLayer_regionY = Integer.MIN_VALUE;

    // use modifyconstant because it's easier than injection
    @Dynamic @ModifyConstant(method = "renderChunkLayer", constant = @Constant(intValue = Integer.MIN_VALUE, ordinal = 0))
    private int initRegionX(int orig) {
        renderChunkLayer_regionY = orig;
        return orig;
    }

    // targets this: if (regionX != renderchunk.regionX || regionZ != renderchunk.regionZ) {
    @Dynamic @Redirect(method = "renderChunkLayer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;regionX:I",
                    ordinal = 0
            ), remap = false)
    private int getHackedRegionX(RenderChunk rc) {
        int regionY = renderChunkLayer_regionY;
        int rcRegY = ((IOptifineRenderChunk) rc).getRegionY();
        if (regionY != rcRegY) {
            return 1; // this value is not possible, so the comparison will always fail
        }
        return ((IOptifineRenderChunk) rc).getRegionX();
    }


    @Group(name = "preRenderRegion", min = 1, max = 2)
    @Dynamic @ModifyArg(method = "preRenderRegion",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"),
            index = 1,
            remap = false,
            require = 0
    )
    private float drawRegionRedirect_deobf(float zero) {
        return (float) (renderChunkLayer_regionY - this.viewEntityY);
    }

    @Group(name = "preRenderRegion", min = 1, max = 2)
    @Dynamic @ModifyArg(method = "preRenderRegion",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;func_179109_b(FFF)V"),
            index = 1,
            remap = false,
            require = 0
    )
    private float drawRegionRedirect_obf(float zero) {
        return (float) (renderChunkLayer_regionY - this.viewEntityY);
    }

    // targets "regionX = renderchunk.regionX;" in the if() targetted above
    // using redirect saves a fragile local capture
    @Dynamic @Redirect(method = "renderChunkLayer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;regionX:I",
                    ordinal = 1
            ), remap = false)
    private int updateRegionY(RenderChunk rc) {
        renderChunkLayer_regionY = ((IOptifineRenderChunk) rc).getRegionY();
        return ((IOptifineRenderChunk) rc).getRegionX();
    }
}
