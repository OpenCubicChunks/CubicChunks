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
package io.github.opencubicchunks.cubicchunks.core.world;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.BitArray;

import java.util.BitSet;

public class NewServerHeightMap implements IHeightMap {

    // 16 entries per level -> 4 bits per level -> 8 levels = 4 bytes

    // scale 0 -> 16 blocks (4 bits)
    // scale 1 -> 256 blocks (8 bits)
    // scale 2 -> 4096 blocks (12 bits)
    // scale 3 -> 65536 blocks (16 bits)
    // scale 4 -> ... (20 bits)
    // scale 5 -> ... (24 bits)
    // scale 6 -> ... (28 bits)
    // scale 7 -> ... (32 bits)

    @SuppressWarnings("unchecked")
    private Int2ObjectMap<HeightMap>[] heightmapsByScale = new Int2ObjectOpenHashMap[8];

    public NewServerHeightMap() {
        for (int i = 0; i < heightmapsByScale.length; i++) {
            heightmapsByScale[i] = new Int2ObjectOpenHashMap<>();
        }
    }

    public void addCube(ICube cube) {

    }

    public void unloadCube(ICube cube) {

    }

    @Override public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        int cubeY = blockToCube(blockY);
        if (opacity > 0) {
            int localY = blockToLocal(blockY);
        }
    }

    @Override public int getTopBlockY(int localX, int localZ) {
        return 0;
    }

    @Override public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        return 0;
    }

    @Override public int getLowestTopBlockY() {
        return 0;
    }

    private class HeightMap {
        private final BitArray heights;
        private final BitSet invalidatedPositions;
        private final int scale, scaledY;

        private HeightMap(int scale, int scaledY) {
            this.heights = new BitArray(5 + scale*4, ICube.SIZE * ICube.SIZE);
            this.invalidatedPositions = new BitSet(ICube.SIZE * ICube.SIZE);
            this.scale = scale;
            this.scaledY = scaledY;
        }
    }
}
