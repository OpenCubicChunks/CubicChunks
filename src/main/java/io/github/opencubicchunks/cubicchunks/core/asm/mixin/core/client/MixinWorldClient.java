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

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(WorldClient.class)
@Implements(@Interface(iface = ICubicWorldInternal.Client.class, prefix = "world$"))
public abstract class MixinWorldClient extends MixinWorld implements ICubicWorldInternal.Client {

    @Shadow private ChunkProviderClient clientChunkProvider;

    @Override public void initCubicWorldClient(IntRange heightRange, IntRange generationRange) {
        super.initCubicWorld(heightRange, generationRange);
        this.isCubicWorld = true;
        CubeProviderClient cubeProviderClient = new CubeProviderClient(this);
        this.chunkProvider = cubeProviderClient;
        this.clientChunkProvider = cubeProviderClient;
    }

    @Override public void tickCubicWorld() {
        getLightingManager().onTick();
    }

    @Override public CubeProviderClient getCubeCache() {
        return (CubeProviderClient) this.clientChunkProvider;
    }
    
    @Override public void setHeightBounds(int minHeight1, int maxHeight1) {
        this.minHeight = minHeight1;
        this.maxHeight = maxHeight1;
    }
}
