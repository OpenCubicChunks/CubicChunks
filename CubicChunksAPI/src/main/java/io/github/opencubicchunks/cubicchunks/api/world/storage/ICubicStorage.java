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
package io.github.opencubicchunks.cubicchunks.api.world.storage;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Representation of a storage format driver for Cubic Chunks worlds.
 * <p>
 * Implementations of this class must be thread-safe.
 * <p>
 * If multiple writes are issued for the same cube/column at once, it is undefined which one will be kept.
 */
@ParametersAreNonnullByDefault
public interface ICubicStorage extends Flushable, AutoCloseable {
    /**
     * Checks whether or not the column at the given position exists.
     *
     * @param pos the column's position
     * @return whether or not the column at the given position exists
     */
    boolean columnExists(ChunkPos pos) throws IOException;

    /**
     * Checks whether or not the cube at the given position exists.
     *
     * @param pos the cube's position
     * @return whether or not the cube at the given position exists
     */
    boolean cubeExists(CubePos pos) throws IOException;

    /**
     * Checks for the existence of multiple cubes+columns at once.
     *
     * @param positions a {@link PosBatch} containing the positions of all the cubes+columns to check for
     * @return a {@link PosBatch} containing the positions of all the cubes+columns that exist
     */
    @Nonnull
    default PosBatch existsBatch(PosBatch positions) throws IOException {
        //default implementation: check positions individually, but in parallel
        try {
            return new PosBatch(
                    positions.columns.parallelStream().filter(pos -> {
                        try {
                            return this.columnExists(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toSet()),
                    positions.cubes.parallelStream().filter(pos -> {
                        try {
                            return this.cubeExists(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toSet()));
        } catch (UncheckedIOException e) {
            throw e.getCause(); //rethrow original exception
        }
    }

    /**
     * Reads the NBT data for the column at the given position.
     *
     * @param pos the column's position
     * @return the column's NBT data, or {@code null} if the column couldn't be found
     */
    NBTTagCompound readColumn(ChunkPos pos) throws IOException;

    /**
     * Reads the NBT data for the cube at the given position.
     *
     * @param pos the cube's position
     * @return the cube's NBT data, or {@code null} if the cube couldn't be found
     */
    NBTTagCompound readCube(CubePos pos) throws IOException;

    /**
     * Reads the NBT data for multiple cubes+columns at once.
     *
     * @param positions a {@link PosBatch} containing the positions of all the cubes+columns to read
     * @return a {@link NBTBatch} containing all the given cube+column positions mapped to their corresponding NBT data, or {@code null} for cubes/columns that can't be found
     */
    @Nonnull
    default NBTBatch readBatch(PosBatch positions) throws IOException {
        //default implementation: issue reads individually, but in parallel
        try {
            return new NBTBatch(
                    positions.columns.parallelStream().collect(Collectors.toConcurrentMap(pos -> pos, pos -> {
                        try {
                            return this.readColumn(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })),
                    positions.cubes.parallelStream().collect(Collectors.toConcurrentMap(pos -> pos, pos -> {
                        try {
                            return this.readCube(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })));
        } catch (UncheckedIOException e) {
            throw e.getCause(); //rethrow original exception
        }
    }

    /**
     * Writes the NBT data to the column at the given position.
     *
     * @param pos the column's position
     * @param nbt the column's NBT data
     */
    void writeColumn(ChunkPos pos, NBTTagCompound nbt) throws IOException;

    /**
     * Writes the NBT data to the cube at the given position.
     *
     * @param pos the cube's position
     * @param nbt the cube's NBT data
     */
    void writeCube(CubePos pos, NBTTagCompound nbt) throws IOException;

    /**
     * Writes the NBT data for multiple cubes+columns at once.
     *
     * @param batch a {@link NBTBatch} containing the cube+column positions and the NBT data to write to each
     */
    default void writeBatch(NBTBatch batch) throws IOException {
        //default implementation: issue writes individually, but in parallel
        try {
            batch.columns.entrySet().parallelStream().forEach(entry -> {
                try {
                    this.writeColumn(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            batch.cubes.entrySet().parallelStream().forEach(entry -> {
                try {
                    this.writeCube(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause(); //rethrow original exception
        }
    }

    /**
     * Iterates over all the columns that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachColumn(Consumer<ChunkPos> callback) throws IOException;

    /**
     * Iterates over all the cubes that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachCube(Consumer<CubePos> callback) throws IOException;

    /**
     * Forces any internally buffered data to be written to disk immediately, blocking until the action is completed.
     * <p>
     * Once this method returns, all writes issued at the time of this method's invocation are guaranteed to be present on disk.
     */
    @Override
    void flush() throws IOException;

    /**
     * Closes this storage.
     * <p>
     * This method may only be called once for a given of {@link ICubicStorage}. Once called, the instance shall be considered to have been disposed, and the behavior of
     * all other methods is undefined.
     */
    @Override
    void close() throws IOException;

    /**
     * A group of positions for both columns and cubes.
     * <p>
     * Used for bulk I/O operations.
     */
    class PosBatch {
        public final Set<ChunkPos> columns;
        public final Set<CubePos> cubes;

        public PosBatch(Set<ChunkPos> columns, Set<CubePos> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }

    /**
     * A group of position+NBT data pairs for both column and cube data.
     * <p>
     * Used for bulk I/O operations.
     *
     * @author DaPorkchop_
     */
    class NBTBatch {
        public final Map<ChunkPos, NBTTagCompound> columns;
        public final Map<CubePos, NBTTagCompound> cubes;

        public NBTBatch(Map<ChunkPos, NBTTagCompound> columns, Map<CubePos, NBTTagCompound> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }
}
