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
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.region.ShadowPagingRegion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ICubicStorage} for the Cubic Chunks' standard Anvil3d storage format.
 */
public class RegionCubeStorage implements ICubicStorage {
    private static SaveCubeColumns saveForPath(Path path) throws IOException {
        if (CubicChunksConfig.useShadowPagingIO) {
            Utils.createDirectories(path);

            Path part2d = path.resolve("region2d");
            Utils.createDirectories(part2d);

            Path part3d = path.resolve("region3d");
            Utils.createDirectories(part3d);

            @SuppressWarnings("unchecked") SaveSection2D section2d = new SaveSection2D(
                    new SharedCachedRegionProvider<>(
                            new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d, (keyProv, r) ->
                                    ShadowPagingRegion.<EntryLocation2D>builder()
                                            .setDirectory(part2d)
                                            .setRegionKey(r)
                                            .setKeyProvider(keyProv)
                                            .setSectorSize(512)
                                            .build(),
                                    (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
                            )
                    ),
                    new SharedCachedRegionProvider<>(
                            new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d,
                                    (keyProvider, regionKey) -> new ExtRegion<>(part2d, Collections.emptyList(), keyProvider, regionKey),
                                    (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
                            )
                    ));
            @SuppressWarnings("unchecked") SaveSection3D section3d = new SaveSection3D(
                    new SharedCachedRegionProvider<>(
                            new SimpleRegionProvider<>(new EntryLocation3D.Provider(), part3d, (keyProv, r) ->
                                    ShadowPagingRegion.<EntryLocation3D>builder()
                                            .setDirectory(part3d)
                                            .setRegionKey(r)
                                            .setKeyProvider(keyProv)
                                            .setSectorSize(512)
                                            .build(),
                                    (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
                            )
                    ),
                    new SharedCachedRegionProvider<>(
                            new SimpleRegionProvider<>(new EntryLocation3D.Provider(), part3d,
                                    (keyProvider, regionKey) -> new ExtRegion<>(part3d, Collections.emptyList(), keyProvider, regionKey),
                                    (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
                            )
                    ));

            return new SaveCubeColumns(section2d, section3d);
        } else {
            return SaveCubeColumns.create(path);
        }
    }

    private final Path path;
    private SaveCubeColumns save;

    public RegionCubeStorage(Path path) throws IOException {
        this.path = Objects.requireNonNull(path, "path");
        this.save = saveForPath(path);
    }

    @Override
    public boolean columnExists(ChunkPos pos) throws IOException {
        return this.save.getSaveSection2D().hasEntry(new EntryLocation2D(pos.x, pos.z));
    }

    @Override
    public boolean cubeExists(CubePos pos) throws IOException {
        return this.save.getSaveSection3D().hasEntry(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public NBTTagCompound readColumn(ChunkPos pos) throws IOException {
        //we use a true here in order to force creation and caching of the new region, thus avoiding an expensive Files.exists() check for every cube/column (which
        // is really expensive on windows)
        Optional<ByteBuffer> data = this.save.load(new EntryLocation2D(pos.x, pos.z), true);
        return data.isPresent()
                ? CompressedStreamTools.readCompressed(new ByteArrayInputStream(data.get().array())) //decompress and parse NBT
                : null; //column doesn't exist
    }

    @Override
    public NBTTagCompound readCube(CubePos pos) throws IOException {
        //see comment in readColumn
        Optional<ByteBuffer> data = this.save.load(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()), true);
        return data.isPresent()
                ? CompressedStreamTools.readCompressed(new ByteArrayInputStream(data.get().array())) //decompress and parse NBT
                : null; //cube doesn't exist
    }

    @Override
    public void writeColumn(ChunkPos pos, NBTTagCompound nbt) throws IOException {
        ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();
        try {
            //compress NBT data
            CompressedStreamTools.writeCompressed(nbt, new ByteBufOutputStream(compressedBuf));

            //write compressed data to disk
            this.save.save2d(new EntryLocation2D(pos.x, pos.z), compressedBuf.nioBuffer());
        } finally {
            compressedBuf.release();
        }
    }

    @Override
    public void writeCube(CubePos pos, NBTTagCompound nbt) throws IOException {
        ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();
        try {
            //compress NBT data
            CompressedStreamTools.writeCompressed(nbt, new ByteBufOutputStream(compressedBuf));

            //write compressed data to disk
            this.save.save3d(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()), compressedBuf.nioBuffer());
        } finally {
            compressedBuf.release();
        }
    }

    @Override
    public void writeBatch(NBTBatch batch) throws IOException {
        Map<EntryLocation2D, ByteBuf> compressedColumns = Collections.emptyMap();
        Map<EntryLocation3D, ByteBuf> compressedCubes = Collections.emptyMap();
        try {
            //compress NBT data
            compressedColumns = this.compressNBTForBatchWrite(batch.columns, pos -> new EntryLocation2D(pos.x, pos.z));
            compressedCubes = this.compressNBTForBatchWrite(batch.cubes, pos -> new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()));

            //write compressed data to disk
            if (!compressedColumns.isEmpty()) {
                this.save.save2d(compressedColumns.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().nioBuffer())));
            }
            if (!compressedCubes.isEmpty()) {
                this.save.save3d(compressedCubes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().nioBuffer())));
            }
        } finally {
            compressedColumns.values().forEach(ByteBuf::release);
            compressedCubes.values().forEach(ByteBuf::release);
        }
    }

    private <KI, KO> Map<KO, ByteBuf> compressNBTForBatchWrite(Map<KI, NBTTagCompound> nbt, Function<KI, KO> keyMappingFunction) throws IOException {
        if (nbt.isEmpty()) { //avoid somewhat expensive stream creation if there are no entries
            return Collections.emptyMap();
        }

        try {
            //if the following code throws an exception, something is VERY wrong, so i won't bother with needlessly complex code to ensure that any
            //  previously allocated buffers get released in the event of an exception being thrown

            return nbt.entrySet().parallelStream().collect(Collectors.toMap(entry -> keyMappingFunction.apply(entry.getKey()), entry -> {
                ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();
                try {
                    //encode and compress nbt data
                    CompressedStreamTools.writeCompressed(entry.getValue(), new ByteBufOutputStream(compressedBuf));

                    return compressedBuf.retain();
                } catch (IOException e) {
                    //wrap exception so that we can throw it from inside the lambda
                    throw new UncheckedIOException(e);
                } finally {
                    compressedBuf.release();
                }
            }));
        } catch (UncheckedIOException e) {
            //rethrow original exception
            throw e.getCause();
        }
    }

    @Override
    public void forEachColumn(Consumer<ChunkPos> callback) throws IOException {
        this.save.getSaveSection2D().forAllKeys(pos -> callback.accept(new ChunkPos(pos.getEntryX(), pos.getEntryZ())));
    }

    @Override
    public void forEachCube(Consumer<CubePos> callback) throws IOException {
        this.save.getSaveSection3D().forAllKeys(pos -> callback.accept(new CubePos(pos.getEntryX(), pos.getEntryY(), pos.getEntryZ())));
    }

    @Override
    public void flush() throws IOException {
        this.save.flush();
    }

    @Override
    public void close() throws IOException {
        this.save.close();
        this.save = null;
    }
}
