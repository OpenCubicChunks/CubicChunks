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

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IThreadedFileIO;

import java.io.IOException;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

public interface ICubeIO extends IThreadedFileIO {
	void flush() throws IOException;

	@Nullable PartialData<Chunk> loadColumnAsyncPart(int chunkX, int chunkZ) throws IOException;

	@Nullable void loadColumnSyncPart(PartialData<Chunk> info);

	@Nullable PartialData<ICube> loadCubeAsyncPart(Chunk column, int cubeY) throws IOException;

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
		final NBTTagCompound nbt;
		final T object;

		PartialData(T object, NBTTagCompound nbt) {
			this.object = object;
			this.nbt = nbt;
		}

		public T getObject() {
			return object;
		}

		public NBTTagCompound getNbt() {
			return nbt;
		}
	}
}
