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
package io.github.opencubicchunks.cubicchunks.api.world.storage;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

import java.io.IOException;
import java.nio.file.Path;

public abstract class StorageFormatProviderBase implements IForgeRegistryEntry<StorageFormatProviderBase> {
    public static final ResourceLocation DEFAULT = new ResourceLocation("cubicchunks", "anvil3d");
    public static IForgeRegistry<StorageFormatProviderBase> REGISTRY;

    public static void init() {
        REGISTRY = new RegistryBuilder<StorageFormatProviderBase>()
                .setType(StorageFormatProviderBase.class)
                .setIDRange(0, 256)
                .setName(new ResourceLocation("cubicchunks", "storage_format_provider_registry"))
                .create();
    }

    public static ResourceLocation defaultStorageFormatProviderName(String fallback) {
        ResourceLocation[] providersThatCanBeDefault = REGISTRY.getValuesCollection().stream()
                .filter(StorageFormatProviderBase::canBeDefault)
                .map(StorageFormatProviderBase::getRegistryName)
                .toArray(ResourceLocation[]::new);
        return providersThatCanBeDefault.length == 1 ? providersThatCanBeDefault[0] :
                fallback.isEmpty() ? DEFAULT : new ResourceLocation(fallback);
    }

    public ResourceLocation registryName;
    public String unlocalizedName;

    @Override
    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    @Override
    public StorageFormatProviderBase setRegistryName(ResourceLocation registryNameIn) {
        this.registryName = registryNameIn;
        return this;
    }

    @Override
    public Class<StorageFormatProviderBase> getRegistryType() {
        return StorageFormatProviderBase.class;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public StorageFormatProviderBase setUnlocalizedName(String nameIn) {
        this.unlocalizedName = nameIn;
        return this;
    }

    public abstract ICubicStorage provideStorage(World world, Path path) throws IOException;

    /**
     * @return whether or not this storage format may be used as the default
     */
    public boolean canBeDefault() {
        return false;
    }
}
