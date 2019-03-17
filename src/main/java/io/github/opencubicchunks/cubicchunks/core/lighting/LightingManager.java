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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: extract interfaces when it's done
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager implements ILightingManager {

    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;

    private final Set<io.github.opencubicchunks.relight.util.ChunkPos> firstLightTodo = new HashSet<>();

    public LightingManager(World world) {

    }

    @Override public void scheduleFirstLight(ICube cube) {

    }

    @Override public void update() {

    }

    @Nullable
    @Override public ICubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
        return null;
    }

    @Override public void registerHeightChangeListener(IHeightChangeListener playerCubeMap) {

    }

    @Override public void onLoadCubeFromNBT(Cube cube, NBTTagCompound lightingInfo) {
        /*
        int[] lastHeightMap = lightingInfo.getIntArray("LastHeightMap"); // NO NO NO! TODO: Why is hightmap being stored in Cube's data?! kill it!
        int[] currentHeightMap = cube.getColumn().getHeightMap();
        byte edgeNeedSkyLightUpdate = 0x3F;
        if (lightingInfo.hasKey("EdgeNeedSkyLightUpdate"))
            edgeNeedSkyLightUpdate = lightingInfo.getByte("EdgeNeedSkyLightUpdate");
        for (int i = 0; i < cube.edgeNeedSkyLightUpdate.length; i++) {
            cube.edgeNeedSkyLightUpdate[i] = (edgeNeedSkyLightUpdate >>> i & 1) == 1;
        }

        // assume changes outside of this cube have no effect on this cube.
        // In practice changes up to 15 blocks above can affect it,
        // but it will be fixed by lighting update in other cube anyway
        int minBlockY = Coords.cubeToMinBlock(cube.getY());
        int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
        for (int i = 0; i < currentHeightMap.length; i++) {
            int currentY = currentHeightMap[i];
            int lastY = lastHeightMap[i];

            //sort currentY and lastY
            int minUpdateY = Math.min(currentY, lastY);
            int maxUpdateY = Math.max(currentY, lastY);

            boolean needLightUpdate = minUpdateY != maxUpdateY &&
                    //if max update Y is below minY - nothing to update
                    !(maxUpdateY < minBlockY) &&
                    //if min update Y is above maxY - nothing to update
                    !(minUpdateY > maxBlockY);
            if (needLightUpdate) {

                //clamp min/max update Y to be within current cube bounds
                if (minUpdateY < minBlockY) {
                    minUpdateY = minBlockY;
                }
                if (maxUpdateY > maxBlockY) {
                    maxUpdateY = maxBlockY;
                }
                assert minUpdateY <= maxUpdateY : "minUpdateY > maxUpdateY: " + minUpdateY + ">" + maxUpdateY;

                int localX = i & 0xF;
                int localZ = i >> 4;
                lightManager.markCubeBlockColumnForUpdate(cube,
                        localToBlock(cube.getX(), localX), localToBlock(cube.getZ(), localZ));
            }
        }*/
    }

    @Override public void onSetBlockState(Chunk chunk, int localX, int oldHeightValue, int y, int localZ) {

    }

    @Override public void onHeightMapUpdate(BlockPos pos) {

    }
}
