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
public interface ICubeProviderServer extends ICubeProvider {

    /**
     * Retrieve a column. The work done to retrieve the column is specified by the {@link Requirement} {@code req}
     *
     * @param columnX Column x position
     * @param columnZ Column z position
     * @param req Work done to retrieve the column
     *
     * @return the column, or {@code null} if no column could be created with the specified requirement level
     */
    @Nullable
    Chunk getColumn(int columnX, int columnZ, Requirement req);

    /**
     * Retrieve a cube. The work done to retrieve the cube is specified by {@link Requirement} {@code req}
     *
     * @param cubeX the cube's x coordinate
     * @param cubeY the cube's y coordinate
     * @param cubeZ the cube's z coordinate
     * @param req what the requirments are before you get the Cube
     *
     * @return the Cube or null if no Cube could be found or created
     */
    @Nullable
    ICube getCube(int cubeX, int cubeY, int cubeZ, Requirement req);

    @Nullable ICube getCubeNow(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req);


    /**
     * Returns true if the specified cube has been already generated (either loaded or saved
     * on disk).
     *
     * @param cubeX x coordinate of the cube
     * @param cubeY y coordinate of the cube
     * @param cubeZ z coordinate of the cube
     * @return true iff, for this position, {@link #getCube(int, int, int, Requirement)} with {@link Requirement#LOAD}
     * would return a non-null value. The result is guaranteed to be true only if the world save is not corrupted
     * and can otherwise be correctly read.
     */
    boolean isCubeGenerated(int cubeX, int cubeY, int cubeZ);

    /**
     * The effort made to retrieve a cube or column. Any further work should not be done, and returning
     * {@code null} is acceptable in those cases
     */
    enum Requirement {
        // Warning, don't modify order of these constants - ordinals are used in comparisons
        // TODO write a custom compare method
        /**
         * Only retrieve the cube/column if it is already cached
         */
        GET_CACHED,
        /**
         * Load the cube/column from disk, if necessary
         */
        LOAD,
        /**
         * Generate the cube/column, if necessary
         */
        GENERATE,
        /**
         * Populate the cube/column, if necessary
         */
        POPULATE,
        /**
         * Generate lighting information for the cube, if necessary
         */
        LIGHT
    }
}
