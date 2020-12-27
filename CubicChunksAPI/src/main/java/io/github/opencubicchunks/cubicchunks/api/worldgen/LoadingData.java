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
package io.github.opencubicchunks.cubicchunks.api.worldgen;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

import javax.annotation.Nullable;

public class LoadingData<POS> {
    private final POS pos;
    @Nullable private NBTTagCompound nbt;

    public LoadingData(POS pos, @Nullable NBTTagCompound nbt) {
        this.pos = pos;
        this.nbt = nbt;
    }

    public POS getPos() {
        return pos;
    }

    /**
     * Returns chunk loading NBT data. Null if chunk not found.
     */
    @Nullable public NBTTagCompound getNbt() {
        return nbt;
    }

    public void setNbt(NBTTagCompound tag) {
        this.nbt = tag;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LoadingData<?> that = (LoadingData<?>) o;
        return pos.equals(that.pos);
    }

    @Override public int hashCode() {
        return Objects.hash(pos);
    }

    @Override public String toString() {
        return "LoadingData(" + pos + ')';
    }
}
