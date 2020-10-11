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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.StagingHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

/**
 * Implements the IColumn interface
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = Chunk.class, priority = 2000)
@Implements(@Interface(iface = IColumn.class, prefix = "chunk$"))
public abstract class MixinChunk_Column implements IColumn, IColumnInternal {

    /*
     * WARNING: WHEN YOU RENAME ANY OF THESE 3 FIELDS RENAME CORRESPONDING
     * FIELDS IN "cubicchunks.asm.mixin.core.common.MixinChunk_Cubes" and
     * "cubicchunks.asm.mixin.core.client.MixinChunk_Cubes".
     */
    private CubeMap cubeMap;
    private IHeightMap opacityIndex;
    private Cube cachedCube;
    private StagingHeightMap stagingHeightMap;
    private boolean isColumn;


    @Shadow @Final public int z;

    @Shadow @Final public int x;

    @Shadow @Final private World world;

    @Shadow public boolean unloadQueued;

    @Shadow @Final private int[] heightMap;

    @Override public Cube getLoadedCube(int cubeY) {
        if (cachedCube != null && cachedCube.getY() == cubeY) {
            return cachedCube;
        }
        return getWorld().getCubeCache().getLoadedCube(x, cubeY, z);
    }


    @Override public Cube getCube(int cubeY) {
        if (cachedCube != null && cachedCube.getY() == cubeY) {
            return cachedCube;
        }
        return getWorld().getCubeCache().getCube(x, cubeY, z);
    }


    @Override public void addCube(ICube cube) {
        this.cubeMap.put((Cube) cube);
    }


    @Override public Cube removeCube(int cubeY) {
        if (cachedCube != null && cachedCube.getY() == cubeY) {
            invalidateCachedCube();
        }
        return this.cubeMap.remove(cubeY);
    }

    @Override
    public void removeFromStagingHeightmap(ICube cube) {
        stagingHeightMap.removeStagedCube(cube);
    }

    @Override
    public void addToStagingHeightmap(ICube cube) {
        stagingHeightMap.addStagedCube(cube);
    }

    @Override
    public int getHeightWithStaging(int localX, int localZ) {
        if (!isColumn) {
            return heightMap[localZ << 4 | localX];
        }
        return Math.max(opacityIndex.getTopBlockY(localX, localZ), stagingHeightMap.getTopBlockY(localX, localZ)) + 1;
    }

    private void invalidateCachedCube() {
        cachedCube = null;
    }


    @Override public boolean hasLoadedCubes() {
        return !cubeMap.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T extends World & ICubicWorldInternal> T getWorld() {
        return (T) this.world;
    }

    @Override public boolean shouldTick() {
        for (Cube cube : cubeMap) {
            if (cube.getTickets().shouldTick()) {
                return true;
            }
        }
        return false;
    }


    @Override public IHeightMap getOpacityIndex() {
        return this.opacityIndex;
    }


    @Override public Collection<? extends ICube> getLoadedCubes() {
        return this.cubeMap.all();
    }


    @Override public Iterable<? extends ICube> getLoadedCubes(int startY, int endY) {
        return this.cubeMap.cubes(startY, endY);
    }


    @Override public void preCacheCube(ICube cube) {
        this.cachedCube = (Cube) cube;
    }

    @Override public int getX() {
        return x;
    }

    @Override public int getZ() {
        return z;
    }

    @Override
    public int getHeightValue(int localX, int blockY, int localZ) {
        return getHeightWithStaging(localX, localZ);
    }

    /**
     * @author Barteks2x
     * @reason go through staging heightmap
     */
    @Overwrite
    public int getHeightValue(int localX, int localZ) {
        return getHeightWithStaging(localX, localZ);
    }

    @Intrinsic
    public int chunk$getHeightValue(int localX, int localZ) {
        return getHeightWithStaging(localX, localZ);
    }
}
