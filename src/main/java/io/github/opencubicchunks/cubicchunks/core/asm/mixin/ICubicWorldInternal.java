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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin;

import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightingManager;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightingManager;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.core.IConfigUpdateListener;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Internal ICubicWorld additions.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldInternal extends ICubicWorld {
    /**
     * Updates the world
     */
    void tickCubicWorld();


    /**
     * Returns the {@link ICubeProvider} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    @Override
    ICubeProviderInternal getCubeCache();

    /**
     * Returns the {@link ILightingManager} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    LightingManager getLightingManager();

    @Override
    Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    @Override
    Cube getCubeFromBlockCoords(BlockPos pos);

    public interface Server extends ICubicWorldInternal, ICubicWorldServer {

        /**
         * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
         * are loaded. Cannot be used more than once.
         * @param heightRange
         * @param generationRange
         */
        void initCubicWorldServer(IntRange heightRange, IntRange generationRange);

        @Override
        CubeProviderServer getCubeCache();

        FirstLightProcessor getFirstLightProcessor();

        ChunkGc getChunkGarbageCollector();

    }

    public interface Client extends ICubicWorldInternal {

        /**
         * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
         * are loaded. Cannot be used more than once.
         * @param heightRange
         * @param generationRange
         */
        void initCubicWorldClient(IntRange heightRange, IntRange generationRange);

        CubeProviderClient getCubeCache();

        void setHeightBounds(int minHeight, int maxHeight);
    }
}
