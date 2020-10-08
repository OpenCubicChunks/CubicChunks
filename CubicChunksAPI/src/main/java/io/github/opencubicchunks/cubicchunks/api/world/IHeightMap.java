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

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IHeightMap {

    /**
     * Sets the opacity at the given position to the given value.
     *
     * @param localX local block x-coordinate (0..15)
     * @param blockY global block y-coordinate
     * @param localZ local block z-coordinate (0..15)
     * @param opacity new opacity (0..255)
     */
    void onOpacityChange(int localX, int blockY, int localZ, int opacity);

    /**
     * Returns true if the block at the given position is occluded by a known non-opaque block further up.
     *
     * @param localX local block x-coordinate (0..15)
     * @param blockY global block y-coordinate
     * @param localZ local block z-coordinate (0..15)
     *
     * @return true if there exists a known non-opaque block above the given y-coordinate
     */
    default boolean isOccluded(int localX, int blockY, int localZ) {
        return blockY <= this.getTopBlockY(localX, localZ);
    }

    /**
     * Returns the y-coordinate of the highest non-transparent block in the specified block-column.
     *
     * @param localX local block x-coordinate (0..15)
     * @param localZ local block z-coordinate (0..15)
     *
     * @return Y position of the top non-transparent block, or very low (far below the min world height) if one doesn't
     * exist
     */
    int getTopBlockY(int localX, int localZ);

    /**
     * Returns the y-coordinate of the highest non-transparent block that is below the given blockY.
     *
     * @param localX local block x-coordinate (0..15)
     * @param localZ local block z-coordinate (0..15)
     * @param blockY only positions below or at this Y coordinate will be retuirned
     *
     * @return Y position of the top non-transparent block below blockY, or very low (far below the min world height) if
     * one doesn't exist
     */
    @Deprecated
    int getTopBlockYBelow(int localX, int localZ, int blockY);

    /**
     * Out of the highest non-opaque blocks from all block columns in the column, returns the y-coordinate of the lowest
     * block.
     *
     * @return the minimum of all top block coordinates in this heightmap instance
     */
    int getLowestTopBlockY();

    // This class exists only because I don't want to introduce many off-by-one errors when modifying height tracking code to store
    // height-above-the-top-block instead of height-of-the-top-block (which is done so that the heightmap array can be shared with vanilla)
    final class HeightMap {
        private int[] data;

        public HeightMap(int[] heightmap) {
            this.data = heightmap;
        }

        public int get(int index) {
            return data[index] - 1;
        }

        public void set(int index, int value) {
            data[index] = value + 1;
        }

        public void increment(int index) {
            data[index]++;
        }

        public void decrement(int index) {
            data[index]--;
        }
    }
}
