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
package io.github.opencubicchunks.cubicchunks.core.server;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

public class CubicAnvilChunkLoader extends AnvilChunkLoader {

    private ICubeIO cubeIOValue;
    private final Supplier<ICubeIO> cubeIOSource;

    // cubeIO needs to be supplier and lazy initialized because of the cubic chunks initialization order
    // AnvilChunkLoader is constructed in CubeProviderServer constructor, which is before CubeIO
    // which is inside the CubeProviderServer exists.
    public CubicAnvilChunkLoader(File chunkSaveLocationIn, DataFixer dataFixerIn, Supplier<ICubeIO> cubeIO) {
        super(chunkSaveLocationIn, dataFixerIn);
        this.cubeIOSource = cubeIO;
    }

    private ICubeIO getCubeIO() {
        if (cubeIOValue == null) {
            cubeIOValue = cubeIOSource.get();
        }
        return cubeIOValue;
    }

    @Override @Nullable public Chunk loadChunk(World worldIn, int x, int z) throws IOException {
        ICubeIO.PartialData<Chunk> data = ((ICubeProviderInternal.Server) worldIn.getChunkProvider()).getCubeIO().loadColumnAsyncPart(worldIn, x, z);
        ((ICubeProviderInternal.Server) worldIn.getChunkProvider()).getCubeIO().loadColumnSyncPart(data);
        return data.getObject();
    }

    @Override @Nullable public Object[] loadChunk__Async(World worldIn, int x, int z) throws IOException {
        ICubeIO.PartialData<Chunk> data = ((ICubeProviderInternal.Server) worldIn.getChunkProvider()).getCubeIO().loadColumnAsyncPart(worldIn, x, z);
        return new Object[]{data.getObject(), data.getNbt()};
    }

    @Override public boolean isChunkGeneratedAt(int x, int z) {
        return this.getCubeIO().columnExists(x, z);
    }

    @Override @Nullable protected Chunk checkedReadChunkFromNBT(World worldIn, int x, int z, NBTTagCompound compound) {
        throw new UnsupportedOperationException();
    }

    @Override @Nullable protected Object[] checkedReadChunkFromNBT__Async(World worldIn, int x, int z, NBTTagCompound compound) {
        throw new UnsupportedOperationException();
    }

    @Override public void saveChunk(World worldIn, Chunk chunkIn) {
        getCubeIO().saveColumn(chunkIn);

        for (ICube cube : ((IColumn) chunkIn).getLoadedCubes()) {
            getCubeIO().saveCube((Cube) cube);
        }
    }

    @Override protected void addChunkToPending(ChunkPos pos, NBTTagCompound compound) {
        throw new UnsupportedOperationException();
    }

    @Override public boolean writeNextIO() {
        return getCubeIO().writeNextIO();
    }

    @Override public void saveExtraChunkData(World worldIn, Chunk chunkIn) {
    }

    @Override public void chunkTick() {
    }

    /**
     * Flushes all pending chunks fully back to disk
     */
    @Override public void flush() {
        try {
            getCubeIO().flush();
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
        }
    }

    @Override public void loadEntities(World worldIn, NBTTagCompound compound, Chunk chunk) {
        throw new UnsupportedOperationException();
    }

    @Override public int getPendingSaveCount() {
        // guess what the right value is?
        return getCubeIO().getPendingColumnCount() + getCubeIO().getPendingCubeCount() / 16;
    }
}
