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
package cubicchunks.server.chunkio;

import cubicchunks.CubicChunks;
import cubicchunks.regionlib.IKey;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ThreadedFileIOBase;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RegionCubeIO implements ICubeIO {

    private static final long kB = 1024;
    private static final long MB = kB * 1024;
    private static final Logger LOGGER = CubicChunks.LOGGER;

    @Nonnull private ICubicWorldServer world;
    @Nonnull private SaveCubeColumns save;
    @Nonnull private ConcurrentMap<ChunkPos, SaveEntry<EntryLocation2D>> columnsToSave;
    @Nonnull private ConcurrentMap<CubePos, SaveEntry<EntryLocation3D>> cubesToSave;

    public RegionCubeIO(ICubicWorldServer world) throws IOException {
        this.world = world;
        WorldProvider prov = world.getProvider();

        Path path = this.world.getSaveHandler().getWorldDirectory().toPath();
        if (prov.getSaveFolder() != null) {
            path = path.resolve(prov.getSaveFolder());
        }
        this.save = SaveCubeColumns.create(path);

        // init chunk save queue
        this.columnsToSave = new ConcurrentHashMap<>();
        this.cubesToSave = new ConcurrentHashMap<>();
    }

    @Override public void flush() throws IOException {
        if (columnsToSave.size() != 0 || cubesToSave.size() != 0) {
            LOGGER.error("Attempt to flush() CubeIO when there are remaining cubes to save! Saving remaining cubes to avoid corruption");
            while (this.writeNextIO()) {
                ;
            }
        }

        this.save.close();
        //if (!this.save.isClosed()) {
        //	this.db.close();
        //} else {
        //	err("DB already closed!");
        //}
    }

    @Override @Nullable public Column loadColumn(int chunkX, int chunkZ) throws IOException {
        NBTTagCompound nbt;
        SaveEntry<EntryLocation2D> saveEntry;
        if ((saveEntry = columnsToSave.get(new ChunkPos(chunkX, chunkZ))) != null) {
            nbt = saveEntry.nbt;
        } else {
            // IOException makes using Optional impossible :(
            Optional<ByteBuffer> buf = this.save.load(new EntryLocation2D(chunkX, chunkZ));
            if (!buf.isPresent()) {
                return null;
            }
            nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(buf.get().array()));
        }
        return IONbtReader.readColumn(world, chunkX, chunkZ, nbt);
    }

    @Override @Nullable public ICubeIO.PartialCubeData loadCubeAsyncPart(Column column, int cubeY) throws IOException {

        NBTTagCompound nbt;
        SaveEntry saveEntry;
        if ((saveEntry = this.cubesToSave.get(new CubePos(column.getX(), cubeY, column.getZ()))) != null) {
            nbt = saveEntry.nbt;
        } else {
            // does the database have the cube?
            Optional<ByteBuffer> buf = this.save.load(new EntryLocation3D(column.getX(), cubeY, column.getZ()));
            if (!buf.isPresent()) {
                return null;
            }
            nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(buf.get().array()));
        }

        // restore the cube - async part
        Cube cube = IONbtReader.readCubeAsyncPart(column, column.getX(), cubeY, column.getZ(), nbt);
        if (cube == null) {
            return null;
        }
        return new ICubeIO.PartialCubeData(cube, nbt);
    }

    @Override public void loadCubeSyncPart(ICubeIO.PartialCubeData info) {
        IONbtReader.readCubeSyncPart(info.cube, world, info.nbt);
    }

    @Override public void saveColumn(Column column) {
        // NOTE: this function blocks the world thread
        // make it as fast as possible by offloading processing to the IO thread
        // except we have to write the NBT in this thread to avoid problems
        // with concurrent access to world data structures

        // add the column to the save queue
        this.columnsToSave
                .put(column.getChunkCoordIntPair(), new SaveEntry<>(new EntryLocation2D(column.getX(), column.getZ()), IONbtWriter.write(column)));
        column.markSaved();

        // signal the IO thread to process the save queue
        ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
    }

    @Override public void saveCube(Cube cube) {
        // NOTE: this function blocks the world thread, so make it fast

        this.cubesToSave.put(cube.getCoords(), new SaveEntry<>(new EntryLocation3D(cube.getX(), cube.getY(), cube.getZ()), IONbtWriter.write(cube)));
        cube.markSaved();

        // signal the IO thread to process the save queue
        ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
    }

    @Override
    public boolean writeNextIO() {
        try {
            // NOTE: return true to redo this call (used for batching)

            final int ColumnsBatchSize = 25;
            final int CubesBatchSize = 250;

            int numColumnsSaved = 0;
            int numColumnsRemaining;
            int numColumnBytesSaved = 0;
            int numCubesSaved = 0;
            int numCubesRemaining;
            int numCubeBytesSaved = 0;
            long start = System.currentTimeMillis();

            // save a batch of columns
            Iterator<SaveEntry<EntryLocation2D>> colIt = columnsToSave.values().iterator();
            for (SaveEntry<EntryLocation2D> entry; colIt.hasNext() && numColumnsSaved < ColumnsBatchSize; numColumnsSaved++) {
                entry = colIt.next();
                try {
                    // save the column
                    byte[] data = IONbtWriter.writeNbtBytes(entry.nbt);
                    this.save.save2d(entry.pos, ByteBuffer.wrap(data));
                    //column can be removed from toSave queue only after writing to disk
                    //to avoid race conditions
                    colIt.remove();
                    numColumnBytesSaved += data.length;
                } catch (Throwable t) {
                    LOGGER.error(String.format("Unable to write column (%d, %d)", entry.pos.getEntryX(), entry.pos.getEntryZ()), t);
                }
            }

            boolean hasMoreColumns = colIt.hasNext();

            Iterator<SaveEntry<EntryLocation3D>> cubeIt = cubesToSave.values().iterator();
            // save a batch of cubes

            for (SaveEntry<EntryLocation3D> entry; cubeIt.hasNext() && numCubesSaved < CubesBatchSize; numCubesSaved++) {
                entry = cubeIt.next();
                try {
                    // save the cube
                    byte[] data = IONbtWriter.writeNbtBytes(entry.nbt);
                    try {
                        this.save.save3d(entry.pos, ByteBuffer.wrap(data));
                    } finally {
                        //cube can be removed from toSave queue only after writing to disk
                        //to avoid race conditions
                        cubeIt.remove();
                    }

                    numCubeBytesSaved += data.length;
                } catch (Throwable t) {
                    LOGGER.error(
                            String.format("Unable to write cube %d, %d, %d", entry.pos.getEntryX(), entry.pos.getEntryY(), entry.pos.getEntryZ()), t);
                }
            }
            boolean hasMoreCubes = cubeIt.hasNext();

            numColumnsRemaining = this.columnsToSave.size();
            numCubesRemaining = this.cubesToSave.size();

            long diff = System.currentTimeMillis() - start;
            LOGGER.debug("Wrote {} columns ({} remaining) ({}k) and {} cubes ({} remaining) ({}k) in {} ms",
                    numColumnsSaved, numColumnsRemaining, numColumnBytesSaved / 1024,
                    numCubesSaved, numCubesRemaining, numCubeBytesSaved / 1024, diff
            );

            return hasMoreColumns || hasMoreCubes;
        } catch (Throwable t) {
            LOGGER.error("Exception occurred when saving cubes", t);
            return cubesToSave.size() != 0 || columnsToSave.size() != 0;
        }
    }

    private static class SaveEntry<T extends IKey<?, ?>> {

        private final T pos;
        private final NBTTagCompound nbt;

        SaveEntry(T pos, NBTTagCompound nbt) {
            this.pos = pos;
            this.nbt = nbt;
        }
    }

}
