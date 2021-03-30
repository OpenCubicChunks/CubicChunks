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
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.*;

/**
 * Implementation of {@link ICubeIO} which internally batches cubes/columns together, and forwards them along to a {@link ICubicStorage} on the I/O thread.
 *
 * @author DaPorkchop_
 */
public class AsyncBatchingCubeIO implements ICubeIO {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected final World world;
    protected final ICubicStorage storage;

    protected final Map<ChunkPos, NBTTagCompound> pendingColumns = new ConcurrentHashMap<>();
    protected final Map<CubePos, NBTTagCompound> pendingCubes = new ConcurrentHashMap<>();

    protected volatile boolean open = true;

    public AsyncBatchingCubeIO(World world, ICubicStorage storage) throws IOException {
        this.world = Objects.requireNonNull(world, "world");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    protected void ensureOpen() {
        checkState(this.open, "already closed?!?");
    }

    @Override
    public boolean columnExists(int columnX, int columnZ) {
        ChunkPos pos = new ChunkPos(columnX, columnZ);

        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            return this.pendingColumns.containsKey(pos) || this.storage.columnExists(pos);
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
            return false;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
        CubePos pos = new CubePos(cubeX, cubeY, cubeZ);

        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            return this.pendingCubes.containsKey(pos) || this.storage.cubeExists(pos);
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
            return false;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public PartialData<Chunk> loadColumnNbt(int chunkX, int chunkZ) throws IOException {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);

        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            NBTTagCompound nbt = this.pendingColumns.get(pos);
            if (nbt == null) { //column isn't cached, forward request on to storage
                nbt = this.storage.readColumn(pos);
            }
            if (nbt != null) { //fix column data
                nbt = FMLCommonHandler.instance().getDataFixer().process(FixTypes.CHUNK, nbt);
            }
            return new PartialData<>(null, nbt);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public PartialData<ICube> loadCubeNbt(Chunk column, int cubeY) throws IOException {
        CubePos pos = new CubePos(column.x, cubeY, column.z);

        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            NBTTagCompound nbt = this.pendingCubes.get(pos);
            if (nbt == null) { //cube isn't cached, forward request on to storage
                nbt = this.storage.readCube(pos);
            }
            return new PartialData<>(null, nbt);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void saveColumn(Chunk column) {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            // NOTE: this function blocks the world thread
            // make it as fast as possible by offloading processing to the IO thread
            // except we have to write the NBT in this thread to avoid problems
            // with concurrent access to world data structures

            // add the column to the save queue
            this.pendingColumns.put(column.getPos(), IONbtWriter.write(column));
            column.setModified(false);

            // signal the IO thread to process the save queue
            ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void saveCube(Cube cube) {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            // NOTE: this function blocks the world thread, so make it fast

            this.pendingCubes.put(cube.getCoords(), IONbtWriter.write(cube));
            cube.markSaved();

            // signal the IO thread to process the save queue
            ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int getPendingColumnCount() {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            return this.pendingColumns.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int getPendingCubeCount() {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();

            return this.pendingCubes.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void flush() throws IOException { //only used by "/save-all flush" command
        this.lock.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure write queue is empty
            this.drainQueueBlocking();

            //flush storage
            this.storage.flush();
        } catch (InterruptedException e) {
            CubicChunks.LOGGER.catching(e);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        this.lock.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure write queue is empty
            this.drainQueueBlocking();

            //close storage
            this.storage.close();

            //mark self as closed AFTER finishing everything else
            this.open = false;
        } catch (InterruptedException e) {
            CubicChunks.LOGGER.catching(e);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Blocks the calling thread until the write queues have been completely drained.
     */
    protected void drainQueueBlocking() throws InterruptedException {
        //this has to submit itself to the I/O thread again, and also run in a loop, in order to avoid a potential race condition caused by the fact that ThreadedFileIOBase
        // is incredibly stupid. don't you think that if you're going to make an ASYNCHRONOUS executor, that you'd ensure that the code is ACTUALLY thread-safe? well,
        // if you're mojang, apparently you don't.

        do {
            ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);

            ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
        } while (!this.pendingColumns.isEmpty() || !this.pendingCubes.isEmpty());
    }

    @Override
    public boolean writeNextIO() {
        try {
            //take a snapshot of both queues
            Map<ChunkPos, NBTTagCompound> columnsSnapshot = new Object2ObjectOpenHashMap<>(this.pendingColumns);
            Map<CubePos, NBTTagCompound> cubesSnapshot = new Object2ObjectOpenHashMap<>(this.pendingCubes);

            //forward all tasks to the storage at once
            this.storage.writeBatch(new ICubicStorage.NBTBatch(Collections.unmodifiableMap(columnsSnapshot), Collections.unmodifiableMap(cubesSnapshot)));

            //remove from queue (using remove(key, value) in order to avoid removing entries which have been modified since the snapshot was taken)
            columnsSnapshot.forEach(this.pendingColumns::remove);
            cubesSnapshot.forEach(this.pendingCubes::remove);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return !this.pendingColumns.isEmpty() || !this.pendingCubes.isEmpty();
    }

    // parsing methods
    // these methods don't use the instance state for anything meaningful, so we can just let them do their thing without worrying about locks and whatnot

    @Override
    public void loadColumnAsyncPart(PartialData<Chunk> info, int chunkX, int chunkZ) {
        if (info.getNbt() == null) {
            return;
        }
        Chunk chunk = IONbtReader.readColumn(this.world, chunkX, chunkZ, info.getNbt());
        info.setObject(chunk);
    }

    @Override
    public void loadColumnSyncPart(PartialData<Chunk> info) {
        //no-op
    }

    @Override
    public void loadCubeAsyncPart(PartialData<ICube> info, Chunk column, int cubeY) {
        if (info.getNbt() == null) {
            return;
        }

        // restore the cube - async part
        Cube cube = IONbtReader.readCubeAsyncPart(column, column.x, cubeY, column.z, info.getNbt());
        info.setObject(cube);
    }

    @Override
    public void loadCubeSyncPart(PartialData<ICube> info) {
        IONbtReader.readCubeSyncPart((Cube) info.object, this.world, info.nbt);
    }
}
