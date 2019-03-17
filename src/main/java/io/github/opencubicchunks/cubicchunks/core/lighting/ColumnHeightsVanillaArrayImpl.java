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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.relight.heightmap.HeightMap;

// This class exists only because I don't want to introduce many off-by-one errors when modifying height tracking code to store
// height-above-the-top-block instead of height-of-the-top-block (which is done so that the heightmap array can be shared with vanilla)
public final class ColumnHeightsVanillaArrayImpl implements HeightMap {

    private int[] data;

    public ColumnHeightsVanillaArrayImpl(int[] heightmap) {
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

    @Override public int getTopY(int localX, int localZ) {
        return data[(localZ << 4) | localX];
    }
}
