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

import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.BiConsumer;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.LoadingData;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IThreadedFileIO;

public interface ICubeIO extends Flushable, AutoCloseable, IThreadedFileIO {
	@Override
	void flush() throws IOException;

	@Override
	void close() throws IOException;

	default PartialData<Chunk> loadColumnAsyncPart(World world, int chunkX, int chunkZ) throws IOException {
		PartialData<Chunk> data = loadColumnNbt(chunkX, chunkZ);
		Collection<BiConsumer<? super World, ? super LoadingData<ChunkPos>>> asyncCallbacks = CubeGeneratorsRegistry.getColumnAsyncLoadingCallbacks();
		if (!asyncCallbacks.isEmpty()) {
			ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
			LoadingData<ChunkPos> chunkLoadingData = new LoadingData<>(chunkPos, data.getNbt());
			asyncCallbacks.forEach(cons -> cons.accept(world, chunkLoadingData));
			data.setNbt(chunkLoadingData.getNbt());
		}
		loadColumnAsyncPart(data, chunkX, chunkZ);
		return data;
	}

	PartialData<Chunk> loadColumnNbt(int chunkX, int chunkZ) throws IOException;

	void loadColumnAsyncPart(PartialData<Chunk> info, int chunkX, int chunkZ);

	void loadColumnSyncPart(PartialData<Chunk> info);

	default PartialData<ICube> loadCubeAsyncPart(Chunk column, int cubeY) throws IOException {
		PartialData<ICube> data = loadCubeNbt(column, cubeY);
		Collection<BiConsumer<? super World, ? super LoadingData<CubePos>>> asyncCallbacks = CubeGeneratorsRegistry.getCubeAsyncLoadingCallbacks();
		if (!asyncCallbacks.isEmpty()) {
			CubePos cubePos = new CubePos(column.x, cubeY, column.z);
			LoadingData<CubePos> chunkLoadingData = new LoadingData<>(cubePos, data.getNbt());
			asyncCallbacks.forEach(cons -> cons.accept(column.getWorld(), chunkLoadingData));
			data.setNbt(chunkLoadingData.getNbt());
		}
		loadCubeAsyncPart(data, column, cubeY);
		return data;
	}

	PartialData<ICube> loadCubeNbt(Chunk column, int cubeY) throws IOException;

	void loadCubeAsyncPart(PartialData<ICube> info, Chunk column, int cubeY);

	void loadCubeSyncPart(PartialData<ICube> info);

	void saveColumn(Chunk column);

	void saveCube(Cube cube);

    boolean cubeExists(int cubeX, int cubeY, int cubeZ);

    boolean columnExists(int columnX, int columnZ);

    int getPendingColumnCount();

    int getPendingCubeCount();

    /**
	 * Stores partially read cube, before sync read but after async read
	 */
	class PartialData<T> {
		NBTTagCompound nbt;
		T object;

		public PartialData(T object, NBTTagCompound nbt) {
			this.object = object;
			this.nbt = nbt;
		}

		public T getObject() {
			return object;
		}

	    public void setObject(T obj) {
		    this.object = obj;
	    }

		public NBTTagCompound getNbt() {
			return nbt;
		}

	    public void setNbt(NBTTagCompound nbt) {
		    this.nbt = nbt;
	    }
    }
}
