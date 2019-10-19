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

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

public abstract class VanillaCompatibilityGeneratorProviderBase implements IForgeRegistryEntry<VanillaCompatibilityGeneratorProviderBase> {

    public static final ResourceLocation DEFAULT = new ResourceLocation("cubicchunks", "default");
    public static IForgeRegistry<VanillaCompatibilityGeneratorProviderBase> REGISTRY;

    public static void init() {
        REGISTRY = new RegistryBuilder<VanillaCompatibilityGeneratorProviderBase>()
                .setType(VanillaCompatibilityGeneratorProviderBase.class)
                .setIDRange(0, 256)
                .setName(new ResourceLocation("cubicchunks", "vanilla_compatibility_generators_registry"))
                .setDefaultKey(DEFAULT)
                .create();
    }

    public ResourceLocation registryName;
    public String unlocalizedName;

    @Override
    public VanillaCompatibilityGeneratorProviderBase setRegistryName(ResourceLocation registryNameIn) {
        registryName = registryNameIn;
        return this;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public Class<VanillaCompatibilityGeneratorProviderBase> getRegistryType() {
        return VanillaCompatibilityGeneratorProviderBase.class;
    }

    public VanillaCompatibilityGeneratorProviderBase setUnlocalizedName(String nameIn) {
        unlocalizedName = nameIn;
        return this;
    }

    public String getUnlocalizedName() {
        return unlocalizedName;
    }

    public abstract ICubeGenerator provideGenerator(IChunkGenerator vanillaChunkGenerator, World world);
}
