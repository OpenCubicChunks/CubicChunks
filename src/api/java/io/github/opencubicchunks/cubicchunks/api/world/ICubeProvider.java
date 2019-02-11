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
package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeProvider {

    @Nullable
    ICube getLoadedCube(int cubeX, int cubeY, int cubeZ);

    @Nullable
    ICube getLoadedCube(CubePos coords);

    ICube getCube(int cubeX, int cubeY, int cubeZ);

    ICube getCube(CubePos coords);

    /**
     * Retrieve a column, if it exists and is loaded
     *
     * @param x The x position of the column
     * @param z The z position of the column
     *
     * @return The column, if loaded. Null, otherwise.
     */
    // TODO remove, use vanilla methods
    @Nullable
    Chunk getLoadedColumn(int x, int z); // more strictly define the return type

    Chunk provideColumn(int x, int z);   // more strictly define the return type
}
