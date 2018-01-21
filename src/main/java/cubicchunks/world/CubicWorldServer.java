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
package cubicchunks.world;

import cubicchunks.ConfigUpdateListener;
import cubicchunks.entity.CubicEntityTracker;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.ChunkGc;
import cubicchunks.server.CubeProviderServer;
import cubicchunks.server.PlayerCubeMap;
import cubicchunks.util.IntRange;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface CubicWorldServer extends CubicWorld, ConfigUpdateListener {

    /**
     * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
     * are loaded. Cannot be used more than once.
     * @param heightRange
     * @param generationRange
     */
    void initCubicWorldServer(IntRange heightRange, IntRange generationRange);

    CubeProviderServer getCubeCache();

    PlayerCubeMap getPlayerCubeMap();

    FirstLightProcessor getFirstLightProcessor();

    //field accessors
    boolean getDisableLevelSaving();

    //vanilla methods
    @Nullable Biome.SpawnListEntry getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos);

    boolean canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos);

    CubicEntityTracker getCubicEntityTracker();
    
    ChunkGc getChunkGarbageCollector();
}
