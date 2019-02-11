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
package io.github.opencubicchunks.cubicchunks.core.worldgen.generator;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

/**
 * Configuration values for the CubicChunks custom terrain generator
 */
public class GlobalGeneratorConfig {

    /**
     * Elevation for sea level
     */
    public static final int SEA_LEVEL = 64;

    /**
     * Maximum elevation for generated terrain
     */
    public static final double MAX_ELEV = 200;

    // TODO add javadoc here
    // these are constants. Changing them may cause issues.
    public static final int X_SECTION_SIZE = 4 + 1;
    public static final int Y_SECTION_SIZE = 8 + 1;
    public static final int Z_SECTION_SIZE = 4 + 1;

    public static final int X_SECTIONS = Cube.SIZE / (X_SECTION_SIZE - 1) + 1;
    public static final int Y_SECTIONS = Cube.SIZE / (Y_SECTION_SIZE - 1) + 1;
    public static final int Z_SECTIONS = Cube.SIZE / (Z_SECTION_SIZE - 1) + 1;
}
