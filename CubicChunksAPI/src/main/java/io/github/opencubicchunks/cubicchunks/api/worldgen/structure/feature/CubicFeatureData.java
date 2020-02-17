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
package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nonnull;

public class CubicFeatureData extends WorldSavedData {

    private NBTTagCompound tagCompound = new NBTTagCompound();

    public CubicFeatureData(String name) {
        super(name);
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.tagCompound = nbt.getCompoundTag("Features");
    }

    @Override @Nonnull public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("Features", this.tagCompound);

        return compound;
    }

    /**
     * Writes the NBT tag of an instance of this structure type to the internal NBT tag, using the chunkcoordinates as
     * the key
     *
     * @param tag   tag compound instance to write
     * @param cubeX cube X coordinate to use as key
     * @param cubeY cube Y coordinate to use as key
     * @param cubeZ cube Z coordinate to use as key
     */
    public void writeInstance(NBTTagCompound tag, int cubeX, int cubeY, int cubeZ) {
        this.tagCompound.setTag(formatChunkCoords(cubeX, cubeY, cubeZ), tag);
    }

    public static String formatChunkCoords(int chunkX, int chunkY, int chunkZ) {
        return "[" + chunkX + "," + chunkY + "," + chunkZ + "]";
    }

    public NBTTagCompound getTagCompound() {
        return this.tagCompound;
    }
}