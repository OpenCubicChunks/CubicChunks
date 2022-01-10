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

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.localToBlock;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.phosphor.LightingHooks;
import io.github.opencubicchunks.cubicchunks.core.lighting.phosphor.PhosphorLightEngine;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: extract interfaces when it's done
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager implements ILightingManager {

    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
    private final World world;
    private final PhosphorLightEngine lightEngine;
    @Nullable private final FirstLightProcessor firstLightProcessor;

    public LightingManager(World world) {
        this.world = world;
        this.lightEngine = new PhosphorLightEngine(world);
        if (!world.isRemote) {
            this.firstLightProcessor = new FirstLightProcessor();
        } else {
            this.firstLightProcessor = null;
        }
    }

    private CubeLightData getLightData(ICube cube) {
        return (CubeLightData) ((Cube) cube).getCubeLightData();
    }

    @Override public void updateLightBetween(Chunk column, int localX, int y1, int y2, int localZ) {
        LightingHooks.relightSkylightColumn(this.world, column, localX, localZ, y1, y2);
    }

    @Override public void onCubeLoad(ICube cube) {
        LightingHooks.scheduleRelightChecksForCubeBoundaries(world, cube);
        tryScheduleOnLoadHeightChangeRelight(cube);
    }

    @Override public void onCreateCubeStorage(ICube cube, ExtendedBlockStorage storage) {
        LightingHooks.initSkylightForSection(world, cube.getColumn(), storage);
    }

    @Override public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
        lightEngine.scheduleLightUpdate(lightType, pos);
        return true;
    }

    @Override public void processUpdates() {
        lightEngine.processLightUpdates();
    }

    @Override public void processUpdatesOnAccess() {
        // don't do onAccess light updates on the client, only update on tick
        if (!world.isRemote) {
            processUpdates();
        }
    }

    @Override public String getId() {
        return "phosphor_cc";
    }

    @Override public void writeToNbt(ICube cube, NBTTagCompound lightingInfo) {
        int[] lastHeightmap = cube.getColumn().getHeightMap();
        lightingInfo.setIntArray("LastHeightMap", lastHeightmap);
        LightingHooks.writeNeighborLightChecksToNBT(cube, lightingInfo);
    }

    @Override public void readFromNbt(ICube cube, NBTTagCompound lightingInfo) {
        CubeLightData lightData = getLightData(cube);
        lightData.lastHeightMap = lightingInfo.hasKey("LastHeightMap") ? lightingInfo.getIntArray("LastHeightMap") : null;
        if (lightData.lastHeightMap != null) {
            Arrays.fill(lightData.lastSaveHeightMapInfo, 0L);
            for (int i = 0; i < lightData.lastHeightMap.length; i++) {
                int cy = Coords.blockToCube(lightData.lastHeightMap[i] - 1);
                int flags = 0;
                if (cy >= cube.getY()) {
                    flags |= 1;
                }
                if (cy <= cube.getY()) {
                    flags |= 2;
                }
                int idx = i >> 5;
                int bit = (i & 31) << 1;
                long v = lightData.lastSaveHeightMapInfo[idx];
                v |= ((long) flags) << bit;
                lightData.lastSaveHeightMapInfo[idx] = v;
            }
        }
        LightingHooks.readNeighborLightChecksFromNBT(cube, lightingInfo);
    }

    @Override public Cube.ICubeLightTrackingInfo createLightData(ICube cube) {
        return new CubeLightData();
    }

    @Override public boolean hasPendingLightUpdates(ICube cube) {
        return lightEngine.hasLightUpdates();
    }

    @Override public void onHeightUpdate(BlockPos pos) {
        if (!world.isRemote) {
            ((PlayerCubeMap) ((WorldServer) world).getPlayerChunkMap()).heightUpdated(pos.getX(), pos.getZ());
        }
    }

    @Override public void onTrackCubeSurface(ICube cube) {
        if (!world.isRemote) {
            BlockPos min = cube.getCoords().getMinBlockPos();
            BlockPos max = cube.getCoords().getMaxBlockPos();
            for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(min, max.add(1, 1, 1))) {
                ((PlayerCubeMap) ((WorldServer) world).getPlayerChunkMap()).heightUpdated(pos.getX(), pos.getZ());
            }
            tryScheduleOnLoadHeightChangeRelight(cube);
        }
    }

    @Override public void doFirstLight(ICube cube) {
        assert firstLightProcessor != null;
        firstLightProcessor.diffuseSkylight(cube);
    }

    /**
     * If lastHeightMap is not null, update current height map from saved data
     */
    private void tryScheduleOnLoadHeightChangeRelight(ICube cube) {
        CubeLightData data = (CubeLightData) ((Cube) cube).getCubeLightData();
        //checking isSurfaceTracked because external tools could set it, and the heightmap could be wrong
        if(data.lastHeightMap == null || !cube.isSurfaceTracked()) {
            return;
        }

        IColumnInternal column = cube.getColumn();

        // assume changes outside this cube have no effect on this cube.
        // In practice changes up to 15 blocks above can affect it,
        // but it will be fixed by lighting update in other cube anyway
        LightingManager lightManager = (LightingManager) ((ICubicWorldInternal) cube.getWorld()).getLightingManager();
        for (int i = 0; i < data.lastHeightMap.length; i++) {
            int localX = i & 0xF;
            int localZ = i >> 4;

            int currentY = column.getHeightWithStaging(localX, localZ);
            int lastY = data.lastHeightMap[i];

            if (currentY == lastY) {
                continue;
            }
            int minUpdateY = Math.min(currentY, lastY);
            int maxUpdateY = Math.max(currentY, lastY) - 1;
            int maxCubeY = Coords.blockToCube(maxUpdateY);
            int minCubeY = Coords.blockToCube(minUpdateY);
            int cubeY = cube.getY();
            if (minCubeY > cubeY || maxCubeY < cubeY) {
                continue;
            }
            int minLocal = 0;
            int maxLocal = 15;
            if (maxCubeY == cubeY) {
                maxLocal = blockToLocal(maxUpdateY);
            }
            if (minCubeY == cubeY) {
                minLocal = blockToLocal(minUpdateY);
            }
            lightManager.updateLightBetween(cube.getColumn(), localX, localToBlock(cubeY, minLocal), localToBlock(cubeY, maxLocal), localZ);
        }

        data.lastHeightMap = null;
    }

    public static class CubeLightData implements Cube.ICubeLightTrackingInfo {
        public long neighborLightChecksBlock, neighborLightChecksSky;
        /**
         * null value means no update from height change from last save
         */
        public int[] lastHeightMap = null;
        // each position has 2 bits:
        // 00 -> unknown, always save
        // 01 -> height was above the current cube, save if flags changed
        // 10 -> height was below the current cube, save if flags changed
        // 11 -> height was in the current cube, no need to save, if saving is needed the cube will be modified
        public long[] lastSaveHeightMapInfo = new long[8]; // xSize*zSize * 2 bits per entry / Long.SIZE

        @Override public boolean needsSaving(ICube cube) {
            int[] heightmap = cube.getColumn().getHeightMap();
            for (int i = 0; i < heightmap.length; i++) {
                int cy = Coords.blockToCube(heightmap[i] - 1);
                int idx = i >> 5;
                int bit = (i & 31) << 1;
                int flags = (int) ((lastSaveHeightMapInfo[idx] >>> bit) & 3);
                if (flags == 0) {
                    return true;
                }
                int newFlags = 0;
                if (cy >= cube.getY()) {
                    newFlags |= 1;
                }
                if (cy <= cube.getY()) {
                    newFlags |= 2;
                }
                if (flags != newFlags) {
                    return true;
                }
            }
            return false;
        }

        @Override public void markSaved(ICube cube) {
            Arrays.fill(lastSaveHeightMapInfo, 0L);
            int[] heightmap = cube.getColumn().getHeightMap();
            for (int i = 0; i < heightmap.length; i++) {
                int cy = Coords.blockToCube(heightmap[i] - 1);
                int flags = 0;
                if (cy >= cube.getY()) {
                    flags |= 1;
                }
                if (cy <= cube.getY()) {
                    flags |= 2;
                }
                int idx = i >> 5;
                int bit = (i & 31) << 1;
                long v = lastSaveHeightMapInfo[idx];
                v |= ((long) flags) << bit;
                lastSaveHeightMapInfo[idx] = v;
            }
        }
    }
}
