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

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.world.ISurfaceTracker;
import io.github.opencubicchunks.relight.util.LightType;
import io.github.opencubicchunks.relight.world.LightDataReader;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightDataReaderImpl implements LightDataReader {

    private final ExtendedBlockStorage[] cache;
    private final ISurfaceTracker[] heightmapCache;

    private final IBlockAccess world;

    private final int startX;
    private final int startY;
    private final int startZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public LightDataReaderImpl(IBlockAccess world, ICubeProvider cubeProv, io.github.opencubicchunks.relight.util.ChunkPos minPos,
        io.github.opencubicchunks.relight.util.ChunkPos maxPos) {
        this.world = world;
        this.startX = minPos.getX();
        this.startY = minPos.getY();
        this.startZ = minPos.getZ();

        this.sizeX = maxPos.getX() - minPos.getX() + 1;
        this.sizeY = maxPos.getY() - minPos.getY() + 1;
        this.sizeZ = maxPos.getZ() - minPos.getZ() + 1;

        this.cache = new ExtendedBlockStorage[sizeX * sizeY * sizeZ];
        this.heightmapCache = new ISurfaceTracker[sizeX * sizeZ];

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                int cubeX = dx + this.startX;
                int cubeZ = dz + this.startZ;
                Chunk column = cubeProv.getLoadedColumn(cubeX, cubeZ);
                if (column != null) {
                    heightmapCache[colIndexLocal(dx, dz)] = ((IColumn) column).getOpacityIndex();
                }
                for (int dy = 0; dy < sizeY; dy++) {

                    int cubeY = dy + this.startY;
                    ICube cube = cubeProv.getLoadedCube(cubeX, cubeY, cubeZ);
                    if (cube != null) {
                        ExtendedBlockStorage storage = cube.getStorage();
                        this.cache[indexLocal(dx, dy, dz)] = storage;
                    }
                }
            }
        }
    }

    private int indexLocal(int localChunkX, int localChunkY, int localChunkZ) {
        return (localChunkX * sizeY + localChunkY) * sizeZ + localChunkZ;
    }

    private int index(int chunkX, int chunkY, int chunkZ) {
        return indexLocal(chunkX - startX, chunkY - startY, chunkZ - startZ);
    }

    private int indexblock(int blockX, int blockY, int blockZ) {
        return index(blockToCube(blockX), blockToCube(blockY), blockToCube(blockZ));
    }

    private int colIndexLocal(int localChunkX, int localChunkZ) {
        return localChunkX * sizeZ + localChunkZ;
    }

    private int colIndex(int chunkX, int chunkZ) {
        return colIndexLocal(chunkX - startX, chunkZ - startZ);
    }

    private int colIndexblock(int blockX, int blockZ) {
        return colIndex(blockToCube(blockX), blockToCube(blockZ));
    }

    @Override public int getLight(int blockX, int blockY, int blockZ, LightType type) {
        ExtendedBlockStorage storage = cache[indexblock(blockX, blockY, blockZ)];
        if (storage != null) {
            if (type == LightType.SKY) {
                return storage.getSkyLight(blockToLocal(blockX), blockToLocal(blockY), blockToLocal(blockZ));
            } else {
                return storage.getBlockLight(blockToLocal(blockX), blockToLocal(blockY), blockToLocal(blockZ));
            }
        } else {
            return 0;
        }
    }

    @Override public int getLightSource(int blockX, int blockY, int blockZ, LightType type) {
        if (type == LightType.BLOCK) {
            ExtendedBlockStorage storage = cache[indexblock(blockX, blockY, blockZ)];
            if (storage != null) {
                return storage.get(blockToLocal(blockX), blockToLocal(blockY), blockToLocal(blockZ))
                    .getLightValue(world, new BlockPos(blockX, blockY, blockZ));
            } else {
                return 0;
            }
        } else {
            ISurfaceTracker hmap = heightmapCache[colIndexblock(blockX, blockZ)];
            if (hmap != null) {
                return blockY > hmap.getTopY(blockToLocal(blockX), blockToLocal(blockZ)) ? 15 : 0;
            } else {
                return 15;
            }
        }
    }
}
