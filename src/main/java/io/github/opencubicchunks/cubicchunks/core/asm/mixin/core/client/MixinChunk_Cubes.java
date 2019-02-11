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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;


import javax.annotation.ParametersAreNonnullByDefault;

import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;

/**
 * Modifies vanilla code in Chunk to use Cubes. Client side only.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Chunk.class)
public abstract class MixinChunk_Cubes implements IColumn {

    @Shadow public abstract ExtendedBlockStorage[] getBlockStorageArray();

    @Shadow @Final private ExtendedBlockStorage[] storageArrays;
    /*
     * WARNING: WHEN YOU RENAME ANY OF THESE 3 FIELDS RENAME CORRESPONDING
     * FIELDS IN "cubicchunks.asm.mixin.core.common.MixinChunk_Cubes" and
     * "cubicchunks.asm.mixin.core.common.MixinChunk_Columns".
     */
    private CubeMap cubeMap;
    private IHeightMap opacityIndex;
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
        if(false)if (isColumn) {
            throw new UnsupportedOperationException("setting storage arrays it not supported with cubic chunks");
        }
    }
}
