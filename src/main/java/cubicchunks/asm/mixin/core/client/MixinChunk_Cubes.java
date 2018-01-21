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
package cubicchunks.asm.mixin.core.client;


import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.world.SurfaceTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cubicchunks.world.column.CubeMap;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;

/**
 * Modifies vanilla code in Chunk to use Cubes. Client side only.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Chunk.class)
public abstract class MixinChunk_Cubes implements Column {

    /*
     * WARNING: WHEN YOU RENAME ANY OF THESE 3 FIELDS RENAME CORRESPONDING
     * FIELDS IN "cubicchunks.asm.mixin.core.common.MixinChunk_Cubes" and
     * "cubicchunks.asm.mixin.core.common.MixinChunk_Columns".
     */
    private CubeMap cubeMap;
    private SurfaceTracker opacityIndex;
    private Cube cachedCube; // todo: make it always nonnull using BlankCube

    private boolean isColumn = false;

    // ==============================================
    //               generateHeightMap
    // ==============================================

    @Inject(method = "generateHeightMap", at = @At(value = "HEAD"), cancellable = true)
    protected void generateHeightMap_CubicChunks_Cancel(CallbackInfo cbi) {
        if (isColumn) {
            cbi.cancel();
        }
    }
    
    // ==============================================
    //                  fillChunk
    // ==============================================

    @Inject(method = "read", at = @At(value = "HEAD"))
    private void fillChunk_CubicChunks_NotSupported(PacketBuffer buf, int i, boolean flag, CallbackInfo cbi) {
        if (isColumn) {
            throw new UnsupportedOperationException("setting storage arrays it not supported with cubic chunks");
        }
    }

    // ==============================================
    //             enqueueRelightChecks
    // ==============================================

    @Inject(method = "enqueueRelightChecks", at = @At(value = "HEAD"), cancellable = true)
    private void enqueueRelightChecks_CubicChunks_NotSupported(CallbackInfo cbi) {
        if (isColumn) {
            // todo: enqueueRelightChecks
            cbi.cancel();
        }
    }
}
