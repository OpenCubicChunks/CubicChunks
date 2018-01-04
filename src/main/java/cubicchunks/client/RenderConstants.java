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
package cubicchunks.client;

import cubicchunks.world.cube.Cube;

public class RenderConstants {

    public static final int RENDER_CHUNK_SIZE_BIT = 6;
    public static final int RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS = RENDER_CHUNK_SIZE_BIT - 4;
    public static final int RENDER_CHUNK_SIZE = 1 << RENDER_CHUNK_SIZE_BIT;
    public static final int RENDER_CHUNK_BLOCKS_AMOUNT = RENDER_CHUNK_SIZE * RENDER_CHUNK_SIZE * RENDER_CHUNK_SIZE;
    public static final int RENDER_CHUNK_EDGE_BLOCK_AMOUNT =
            RENDER_CHUNK_BLOCKS_AMOUNT - (RENDER_CHUNK_SIZE - 2) * (RENDER_CHUNK_SIZE - 2) * (RENDER_CHUNK_SIZE - 2);
    public static final int RENDER_CHUNK_SIZE_IN_CUBES = RENDER_CHUNK_SIZE / Cube.SIZE;
    public static final int RENDER_CHUNK_MAX_POS = RENDER_CHUNK_SIZE - 1;
    public static final int RENDER_CHUNK_START_POS_MASK = 0xFFFFFFFF ^ RENDER_CHUNK_MAX_POS;
    public static final double RENDER_CHUNK_CENTER_POS = RENDER_CHUNK_SIZE / 2.0D;
    public static final float RENDER_CHUNK_CENTER_POS_FLOAT = RENDER_CHUNK_SIZE / 2.0F;
}
