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
package cubicchunks.worldgen.generator.custom.biome;

import com.google.common.base.Preconditions;

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

import cubicchunks.CubicChunks;
import cubicchunks.worldgen.generator.custom.biome.replacer.IBiomeBlockReplacerProvider;
import cubicchunks.worldgen.generator.custom.biome.replacer.OceanWaterReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.SurfaceDefaultReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.TerrainShapeReplacer;
import cubicchunks.worldgen.generator.custom.features.decorator.ICubicBiomeDecorator;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CubicBiomeType extends IForgeRegistryEntry.Impl<CubicBiomeType> {

	public static final IForgeRegistry<CubicBiomeType> REGISTRY = new RegistryBuilder<CubicBiomeType>()
		.setType(CubicBiomeType.class)
		.setIDRange(0, 255)
		.setName(new ResourceLocation(CubicChunks.MODID, "cubic_biome_registry"))
		.create();
	private static final Map<Biome, CubicBiomeType> biomeMapping = new IdentityHashMap<>();
	private static boolean isPostInit = false;

	private final Biome originalBiome;
	private final List<IBiomeBlockReplacerProvider> blockReplacers = new ArrayList<>();
	private final List<ICubicBiomeDecorator> decorators = new ArrayList<>();

	public Iterable<IBiomeBlockReplacerProvider> getReplacerProviders() {
		return Collections.unmodifiableList(blockReplacers);
	}

	public Iterable<ICubicBiomeDecorator> getDecorators() {
		return Collections.unmodifiableList(decorators);
	}

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
		for (CubicBiomeType cubicBiomeType : CubicBiomeType.REGISTRY) {
			Biome biome = cubicBiomeType.getBiome();
			biomeMapping.put(biome, cubicBiomeType);
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
				CubicChunks.LOGGER.warn("Biome {} not registered as cubic chunks biome, will use default unregistered biome instead", biome.getRegistryName());

				CubicBiomeType newBiome = CubicBiomeType
					.createForBiome(biome)
					.defaults()
					.setRegistryName(CubicChunks.location("unregistered_" + biome.getRegistryName().getResourcePath()))
					.create();
				biomeMapping.put(biome, newBiome);
			}
		}
	}

	private CubicBiomeType(Builder builder) {
		this.originalBiome = builder.biome;
		this.blockReplacers.addAll(builder.blockReplacers);
		this.decorators.addAll(builder.biomeDecorators);
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
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CubicBiomeType that = (CubicBiomeType) o;

		return originalBiome != null ? originalBiome.equals(that.originalBiome) : that.originalBiome == null;

	}

	@Override public int hashCode() {
		return originalBiome != null ? originalBiome.hashCode() : 0;
	}

	public static CubicBiomeType getCubic(Biome vanillaBiome) {
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

	public static CubicBiomeType.Builder createForBiome(Biome biome) {
		return new Builder(biome);
	}

	public static class Builder {
		private final Biome biome;
		private List<IBiomeBlockReplacerProvider> blockReplacers = new ArrayList<>();
		private List<ICubicBiomeDecorator> biomeDecorators = new ArrayList<>();
		private ResourceLocation registryName;

		public Builder(Biome biome) {
			this.biome = biome;
		}

		public Builder defaults() {
			return addDefaultBlockReplacers()
				.addDefaultDecorators();
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

		public Builder addDecorator(ICubicBiomeDecorator decorator) {
			this.biomeDecorators.add(decorator);
			return this;
		}

		public Builder addDefaultDecorators() {
			// TODO: decorators unimplemented yet
			return this;
		}

		public Builder setRegistryName(ResourceLocation registryName) {
			this.registryName = registryName;
			return this;
		}

		public Builder setRegistryName(String modid, String resourcePath) {
			return this.setRegistryName(new ResourceLocation(modid, resourcePath));
		}

		public CubicBiomeType create() {
			if (this.registryName == null) {
				this.registryName = biome.getRegistryName();
			}
			return new CubicBiomeType(this);
		}

		public CubicBiomeType register() {
			CubicBiomeType biome = create();
			GameRegistry.register(biome);
			return biome;
		}
	}
}
