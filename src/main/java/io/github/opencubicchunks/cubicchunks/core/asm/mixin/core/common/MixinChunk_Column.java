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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Implements the IColumn interface
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = Chunk.class, priority = 2000)
@Implements(@Interface(iface = IColumn.class, prefix = "chunk$"))
public abstract class MixinChunk_Column implements IColumn {

    /*
     * WARNING: WHEN YOU RENAME ANY OF THESE 3 FIELDS RENAME CORRESPONDING
     * FIELDS IN "cubicchunks.asm.mixin.core.common.MixinChunk_Cubes" and
     * "cubicchunks.asm.mixin.core.client.MixinChunk_Cubes".
     */
    private CubeMap cubeMap;
    private IHeightMap opacityIndex;
    private Cube cachedCube;

    @Shadow @Final public int z;

    @Shadow @Final public int x;

    @Shadow @Final private World world;

    @Shadow public boolean unloadQueued;

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

    private void invalidateCachedCube() {
        cachedCube = null;
    }


    @Override public boolean hasLoadedCubes() {
        return !cubeMap.isEmpty();
    }

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


    @Override public Collection getLoadedCubes() {
        return this.cubeMap.all();
    }


    @Override public Iterable getLoadedCubes(int startY, int endY) {
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
}
