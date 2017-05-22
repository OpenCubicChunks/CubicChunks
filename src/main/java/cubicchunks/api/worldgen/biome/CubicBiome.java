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
package cubicchunks.api.worldgen.biome;

import com.google.common.base.Preconditions;
import cubicchunks.CubicChunks;
import cubicchunks.api.worldgen.populator.CubicPopulatorList;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.worldgen.generator.custom.biome.replacer.IBiomeBlockReplacerProvider;
import cubicchunks.worldgen.generator.custom.biome.replacer.OceanWaterReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.SurfaceDefaultReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.TerrainShapeReplacer;
import cubicchunks.worldgen.generator.custom.populator.AnimalsPopulator;
import cubicchunks.worldgen.generator.custom.populator.DefaultDecorator;
import cubicchunks.worldgen.generator.custom.populator.PrePopulator;
import cubicchunks.worldgen.generator.custom.populator.SnowBiomeDecorator;
import cubicchunks.worldgen.generator.custom.populator.SurfaceSnowPopulator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

//
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CubicBiome extends IForgeRegistryEntry.Impl<CubicBiome> {

    public static final IForgeRegistry<CubicBiome> REGISTRY = new RegistryBuilder<CubicBiome>()
            .setType(CubicBiome.class)
            .setIDRange(0, 255)
            .setName(new ResourceLocation(CubicChunks.MODID, "cubic_biome_registry"))
            .create();
    private static final Map<Biome, CubicBiome> biomeMapping = new IdentityHashMap<>();
    private static boolean isPostInit = false;

    private final Biome originalBiome;
    private final List<IBiomeBlockReplacerProvider> blockReplacers = new ArrayList<>();
    private ICubicPopulator decorator;

    public Iterable<IBiomeBlockReplacerProvider> getReplacerProviders() {
        return Collections.unmodifiableList(blockReplacers);
    }

    public ICubicPopulator getDecorator() {
        return decorator;
    }

    // INTERNAL USE ONLY
    public static void init() {
        // nothing here, exists just to call static initializer
    }

    public static void postInit() {
        if (isPostInit) {
            return;
        }
        isPostInit = true;

        boolean anyUnregistered = false;
        // make sure that all registered cubic biomes are for biomes that are actually registered
        for (CubicBiome cubicBiome : CubicBiome.REGISTRY) {
            Biome biome = cubicBiome.getBiome();
            biomeMapping.put(biome, cubicBiome);
            if (!ForgeRegistries.BIOMES.containsValue(biome)) {
                anyUnregistered = true;
                CubicChunks.LOGGER.error(
                        "Registered cubic chunks biome has unregistered biome {} (name={}, class={}) is not allowed",
                        biome.getRegistryName(), biome, biome.getBiomeName());
            }
        }
        if (anyUnregistered) {
            throw new IllegalStateException("Found one or more unregistered biomes with registered cubic chunks biomes");
        }

        for (Biome biome : ForgeRegistries.BIOMES) {
            if (!biomeMapping.containsKey(biome)) {
                CubicChunks.LOGGER
                        .warn("Biome {} not registered as cubic chunks biome, will use default unregistered biome instead", biome.getRegistryName());

                CubicBiome newBiome = CubicBiome
                        .createForBiome(biome)
                        .defaults()
                        .defaultDecorators()
                        .setRegistryName(CubicChunks.location("unregistered_" + biome.getRegistryName().getResourcePath()))
                        .create();
                biomeMapping.put(biome, newBiome);
            }
        }
    }

    private CubicBiome(Builder builder) {
        this.originalBiome = builder.biome;
        this.blockReplacers.addAll(builder.blockReplacers);
        this.decorator = builder.decorators;
        this.setRegistryName(builder.registryName);
    }

    public Biome getBiome() {
        return this.originalBiome;
    }

    @Override
    public String toString() {
        return this.getRegistryName().toString();
    }

    // equals and hashcode should only check the biome
    // the only cubic biome with null biome is the unregistered one
    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CubicBiome that = (CubicBiome) o;

        return originalBiome != null ? originalBiome.equals(that.originalBiome) : that.originalBiome == null;

    }

    @Override public int hashCode() {
        return originalBiome != null ? originalBiome.hashCode() : 0;
    }

    public static CubicBiome getCubic(Biome vanillaBiome) {
        return biomeMapping.get(vanillaBiome);
    }

    public static IBiomeBlockReplacerProvider terrainShapeReplacer() {
        return IBiomeBlockReplacerProvider.of(new TerrainShapeReplacer());
    }

    public static IBiomeBlockReplacerProvider oceanWaterReplacer() {
        return OceanWaterReplacer.provider();
    }

    public static IBiomeBlockReplacerProvider surfaceDefaultReplacer() {
        return SurfaceDefaultReplacer.provider();
    }

    public static CubicBiome.Builder createForBiome(Biome biome) {
        return new Builder(biome);
    }

    public static class Builder {

        private final Biome biome;
        private List<IBiomeBlockReplacerProvider> blockReplacers = new ArrayList<>();
        private ResourceLocation registryName;
        private final CubicPopulatorList decorators = new CubicPopulatorList();

        public Builder(Biome biome) {
            this.biome = biome;
        }

        public Builder defaults() {
            return addDefaultBlockReplacers()
                    .defaultDecorators();
        }

        public Builder addDefaultBlockReplacers() {
            return addBlockReplacer(terrainShapeReplacer())
                    .addBlockReplacer(surfaceDefaultReplacer())
                    .addBlockReplacer(oceanWaterReplacer());
        }

        public Builder addBlockReplacer(IBiomeBlockReplacerProvider provider) {
            Preconditions.checkNotNull(provider);
            this.blockReplacers.add(provider);
            return this;
        }

        public Builder defaultDecorators() {
            this.decorator(new PrePopulator());
            this.decorator(new DefaultDecorator.Ores());
            this.decorator(new DefaultDecorator());
            return this;
        }

        public Builder defaultPostDecorators() {
            this.decorator(new AnimalsPopulator());
            this.decorator(new SurfaceSnowPopulator());
            return this;
        }

        public Builder decorator(ICubicPopulator decorator) {
            this.decorators.add(decorator);
            return this;
        }

        public Builder setRegistryName(ResourceLocation registryName) {
            this.registryName = registryName;
            return this;
        }

        public Builder setRegistryName(String modid, String resourcePath) {
            return this.setRegistryName(new ResourceLocation(modid, resourcePath));
        }

        public CubicBiome create() {
            if (this.registryName == null) {
                this.registryName = biome.getRegistryName();
            }
            return new CubicBiome(this);
        }

        public CubicBiome register() {
            CubicBiome biome = create();
            GameRegistry.register(biome);
            return biome;
        }
    }
}
