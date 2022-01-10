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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
// TODO: make this a real API
public interface ILightingManager {

    default void doOnBlockSetLightUpdates(Chunk column, int localX, int y1, int y2, int localZ) {
        updateLightBetween(column, localX, y1, y2, localZ);
    }

    void updateLightBetween(Chunk column, int localX, int y1, int y2, int localZ);

    default void onSendCubes(Iterable<? extends ICube> cubes) {
        processUpdates();
    }

    void onCubeLoad(ICube cube);

    default void onCubeUnload(ICube cube) {
        processUpdatesOnAccess();
    }

    default void onGetLight(EnumSkyBlock type, BlockPos pos) {
        processUpdatesOnAccess();
    }

    default void onGetLightSubtracted(BlockPos pos) {
        processUpdatesOnAccess();
    }

    void onCreateCubeStorage(ICube cube, ExtendedBlockStorage storage);

    default void onTick() {
        processUpdates();
    }

    boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos);

    void processUpdates();

    void processUpdatesOnAccess();

    String getId();

    void writeToNbt(ICube cube, NBTTagCompound lightingInfo);

    void readFromNbt(ICube cube, NBTTagCompound lightingInfo);

    Cube.ICubeLightTrackingInfo createLightData(ICube cube);

    boolean hasPendingLightUpdates(ICube cube);

    void onHeightUpdate(BlockPos pos);

    void onTrackCubeSurface(ICube cube);

    void doFirstLight(ICube cube);
}
