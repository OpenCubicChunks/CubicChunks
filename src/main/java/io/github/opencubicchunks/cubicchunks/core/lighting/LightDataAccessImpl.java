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

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.relight.heightmap.HeightMap;
import io.github.opencubicchunks.relight.util.LightType;
import io.github.opencubicchunks.relight.world.LightChunk;
import io.github.opencubicchunks.relight.world.LightDataAccess;
import io.github.opencubicchunks.relight.world.LightDataReader;
import io.github.opencubicchunks.relight.world.LightDataWriter;
import io.github.opencubicchunks.relight.world.WorldAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.List;

public class LightDataAccessImpl implements WorldAccess, LightDataAccess {

    private final World world;

    public LightDataAccessImpl(World world) {
        this.world = world;
    }

    @Override public LightDataReader getReaderFor(io.github.opencubicchunks.relight.util.ChunkPos minPos, io.github.opencubicchunks.relight.util.ChunkPos maxPos) {
        return new LightDataReaderImpl(world, (ICubeProvider) world.getChunkProvider(), minPos, maxPos);
    }

    @Override public LightDataWriter getWriterFor(io.github.opencubicchunks.relight.util.ChunkPos minPos, io.github.opencubicchunks.relight.util.ChunkPos maxPos) {
        return this;
    }

    @Override public HeightMap getHeightMap(io.github.opencubicchunks.relight.util.ColumnPos pos) {
        return ((IColumn) world.getChunkFromChunkCoords(pos.getX(), pos.getZ())).getOpacityIndex();
    }

    @Override public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ) {
        return world.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), cubeToMinBlock(chunkY), cubeToMinBlock(chunkZ)), false);
    }

    @Override public LightChunk getLightChunk(io.github.opencubicchunks.relight.util.ChunkPos pos) {
        return (Cube) ((ICubicWorld) world).getCubeFromCubeCoords(pos.getX(), pos.getY(), pos.getZ());
    }

    @SuppressWarnings("unchecked") @Override public List<LightChunk> chunksBetween(io.github.opencubicchunks.relight.util.ColumnPos pos, int start, int end) {
        IColumn column = (IColumn) world.getChunkFromChunkCoords(pos.getX(), pos.getZ());
        return (List<LightChunk>) (Object) column.getLoadedCubes(start, end);
    }

    // from LightDataAccess

    @Override public int getLight(int blockX, int blockY, int blockZ, LightType type) {
        return world.getLightFor(type == LightType.SKY ? EnumSkyBlock.SKY : EnumSkyBlock.BLOCK, new BlockPos(blockX, blockY, blockZ));
    }

    @Override public int getLightSource(int blockX, int blockY, int blockZ, LightType type) {
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        if (type == LightType.BLOCK) {
            return world.getBlockState(pos).getLightValue(world, pos);
        } else {
            return world.canBlockSeeSky(pos) ? 15 : 0;
        }
    }

    @Override public void setLight(int blockX, int blockY, int blockZ, int value, LightType type) {
        world.setLightFor(type == LightType.SKY ? EnumSkyBlock.SKY : EnumSkyBlock.BLOCK, new BlockPos(blockX, blockY, blockZ), value);
    }
}
