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
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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

    @Nonnull private World world;
    @Nonnull private SaveCubeColumns save;
    @Nonnull private ConcurrentMap<ChunkPos, SaveEntry<EntryLocation2D>> columnsToSave;
    @Nonnull private ConcurrentMap<CubePos, SaveEntry<EntryLocation3D>> cubesToSave;
    
    public RegionCubeIO(World world) throws IOException {
        this.world = world;

        initSave();

        // init chunk save queue
        this.columnsToSave = new ConcurrentHashMap<>();
        this.cubesToSave = new ConcurrentHashMap<>();
    }

    private void initSave() throws IOException {
        // TODO: make path a constructor argument
        Path path;
        if (world instanceof WorldServer) {
            WorldProvider prov = world.provider;
            path = this.world.getSaveHandler().getWorldDirectory().toPath();
            if (prov.getSaveFolder() != null) {
                path = path.resolve(prov.getSaveFolder());
            }
        } else {
            path = Paths.get(".").toAbsolutePath().resolve("clientCache").resolve("DIM" + world.provider.getDimension());
        }

        Files.createDirectories(path);

        Path part2d = path.resolve("region2d");
        Files.createDirectories(part2d);

        Path part3d = path.resolve("region3d");
        Files.createDirectories(part3d);

        this.save = new SaveCubeColumns(
                new SaveSection2D(
                        new SharedCachedRegionProvider<>(
                                SimpleRegionProvider.createDefault(new EntryLocation2D.Provider(), part2d, 512)
                        ),
                        new SharedCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d,
                                        (keyProvider, regionKey) -> new ExtRegion<>(part2d, Collections.emptyList(), keyProvider, regionKey)
                                )
                        )),
                new SaveSection3D(
                        new SharedCachedRegionProvider<>(
                                SimpleRegionProvider.createDefault(new EntryLocation3D.Provider(), part3d, 512)
                        ),
                        new SharedCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation3D.Provider(), part3d,
                                        (keyProvider, regionKey) -> new ExtRegion<>(part3d, Collections.emptyList(), keyProvider, regionKey)
                                )
                        ))
        );
    }

    @Override public void flush() throws IOException {
        if (columnsToSave.size() != 0 || cubesToSave.size() != 0) {
            LOGGER.error("Attempt to flush() CubeIO when there are remaining cubes to save! Saving remaining cubes to avoid corruption");
            while (this.writeNextIO()) {
                ;
            }
        }

        try {
            this.save.close();
        } catch(IllegalStateException alreadyClosed) {
            // ignore
        } catch (Exception ex) {
            CubicChunks.LOGGER.catching(ex);
        }
        // TODO: hack! fix this properly in RegionLib by adding flush()
        // this avoids Already closed exceptions when vanilla calls flush without the intent to actually close anything
        // This also needs a lot of testing on windows
        this.initSave();
    }

    @Override @Nullable public Chunk loadColumn(int chunkX, int chunkZ) throws IOException {
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
            nbt = FMLCommonHandler.instance().getMinecraftServerInstance().getDataFixer().process(FixTypes.CHUNK, CompressedStreamTools.readCompressed(new ByteArrayInputStream(buf.get().array())));
        }
        return IONbtReader.readColumn(world, chunkX, chunkZ, nbt);
    }

    @Override @Nullable public ICubeIO.PartialCubeData loadCubeAsyncPart(Chunk column, int cubeY) throws IOException {

        NBTTagCompound nbt;
        SaveEntry<EntryLocation3D> saveEntry;
        if ((saveEntry = this.cubesToSave.get(new CubePos(column.xPosition, cubeY, column.zPosition))) != null) {
            nbt = saveEntry.nbt;
        } else {
            // does the database have the cube?
            Optional<ByteBuffer> buf = this.save.load(new EntryLocation3D(column.xPosition, cubeY, column.zPosition));
            if (!buf.isPresent()) {
                return null;
            }
            nbt = FMLCommonHandler.instance().getMinecraftServerInstance().getDataFixer().process(FixTypes.CHUNK, CompressedStreamTools.readCompressed(new ByteArrayInputStream(buf.get().array())));
        }

        // restore the cube - async part
        Cube cube = IONbtReader.readCubeAsyncPart(column, column.xPosition, cubeY, column.zPosition, nbt);
        if (cube == null) {
            return null;
        }
        return new ICubeIO.PartialCubeData(cube, nbt);
    }

    @Override public void loadCubeSyncPart(ICubeIO.PartialCubeData info) {
        IONbtReader.readCubeSyncPart(info.cube, world, info.nbt);
    }

    @Override public void saveColumn(Chunk column) {
        // NOTE: this function blocks the world thread
        // make it as fast as possible by offloading processing to the IO thread
        // except we have to write the NBT in this thread to avoid problems
        // with concurrent access to world data structures

        // add the column to the save queue
        this.columnsToSave
                .put(column.getChunkCoordIntPair(), new SaveEntry<>(new EntryLocation2D(column.xPosition, column.zPosition), IONbtWriter.write(column)));
        column.setModified(false);

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
            int numCubesSaved = 0;

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
                } catch (Throwable t) {
                    LOGGER.error(
                            String.format("Unable to write cube %d, %d, %d", entry.pos.getEntryX(), entry.pos.getEntryY(), entry.pos.getEntryZ()), t);
                }
            }
            boolean hasMoreCubes = cubeIt.hasNext();
            return hasMoreColumns || hasMoreCubes;
        } catch (Throwable t) {
            LOGGER.error("Exception occurred when saving cubes", t);
            return cubesToSave.size() != 0 || columnsToSave.size() != 0;
        }
    }

    private static class SaveEntry<T extends IKey<?>> {

        private final T pos;
        private final NBTTagCompound nbt;

        SaveEntry(T pos, NBTTagCompound nbt) {
            this.pos = pos;
            this.nbt = nbt;
        }
    }

}
