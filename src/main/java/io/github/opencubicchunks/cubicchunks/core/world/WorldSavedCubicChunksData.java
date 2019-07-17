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

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class WorldSavedCubicChunksData extends WorldSavedData {

    public boolean isCubicChunks = false;
    public int minHeight = 0, maxHeight = 256;

    public WorldSavedCubicChunksData(String name) {
        super(name);
    }
    
    public WorldSavedCubicChunksData(String name, boolean isCC) {
        this(name);
        if (isCC) {
            minHeight = CubicChunks.MIN_BLOCK_Y;
            maxHeight = CubicChunks.MAX_BLOCK_Y;
            isCubicChunks = true;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        minHeight = nbt.getInteger("minHeight");
        maxHeight = nbt.getInteger("maxHeight");
        isCubicChunks = !nbt.hasKey("isCubicChunks") || nbt.getBoolean("isCubicChunks");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("minHeight", minHeight);
        compound.setInteger("maxHeight", maxHeight);
        compound.setBoolean("isCubicChunks", isCubicChunks);
        return compound;
    }

}
