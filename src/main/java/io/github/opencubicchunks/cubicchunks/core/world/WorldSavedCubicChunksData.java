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

import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldSavedData;

public class WorldSavedCubicChunksData extends WorldSavedData {

    public boolean isCubicChunks = false;
    public int minHeight = 0, maxHeight = 256;
    public ResourceLocation compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
    public ResourceLocation storageFormat = StorageFormatProviderBase.DEFAULT;

    public WorldSavedCubicChunksData(String name) {
        super(name);
    }
    
    public WorldSavedCubicChunksData(String name, boolean isCC, int minHeight, int maxHeight) {
        this(name);
        if (isCC) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            isCubicChunks = true;
            compatibilityGeneratorType = new ResourceLocation(CubicChunksConfig.compatibilityGeneratorType);
            storageFormat = StorageFormatProviderBase.defaultStorageFormatProviderName(CubicChunksConfig.storageFormat);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        minHeight = nbt.getInteger("minHeight");
        maxHeight = nbt.getInteger("maxHeight");
        isCubicChunks = !nbt.hasKey("isCubicChunks") || nbt.getBoolean("isCubicChunks");
        if(nbt.hasKey("compatibilityGeneratorType"))
            compatibilityGeneratorType = new ResourceLocation(nbt.getString("compatibilityGeneratorType"));
        else
            compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
        if(nbt.hasKey("storageFormat"))
            storageFormat = new ResourceLocation(nbt.getString("storageFormat"));
        else
            storageFormat = StorageFormatProviderBase.defaultStorageFormatProviderName(CubicChunksConfig.storageFormat);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("minHeight", minHeight);
        compound.setInteger("maxHeight", maxHeight);
        compound.setBoolean("isCubicChunks", isCubicChunks);
        compound.setString("compatibilityGeneratorType", compatibilityGeneratorType.toString());
        compound.setString("storageFormat", storageFormat.toString());
        return compound;
    }

}
