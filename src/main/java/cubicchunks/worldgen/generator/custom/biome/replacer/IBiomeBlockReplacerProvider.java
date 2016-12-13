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
package cubicchunks.worldgen.generator.custom.biome.replacer;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.custom.biome.CubicBiome;
import mcp.MethodsReturnNonnullByDefault;

/**
 * Creates new IBiomeBlockReplacer based on supplied configuration,
 * and gives information about all supported configuration options for this replacer.
 * <p>
 * Configuration keys are ResourceLocations
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IBiomeBlockReplacerProvider {
	IBiomeBlockReplacer create(ICubicWorld world, CubicBiome biome, BiomeBlockReplacerConfig conf);

	Set<ConfigOptionInfo> getPossibleConfigOptions();

	static IBiomeBlockReplacerProvider of(Supplier<IBiomeBlockReplacer> supplier) {
		return new IBiomeBlockReplacerProvider() {
			@Override
			public IBiomeBlockReplacer create(ICubicWorld world, CubicBiome biome, BiomeBlockReplacerConfig conf) {
				return supplier.get();
			}

			@Override public Set<ConfigOptionInfo> getPossibleConfigOptions() {
				return Sets.newHashSet();
			}
		};
	}

	static IBiomeBlockReplacerProvider of(IBiomeBlockReplacer replacer) {
		return new IBiomeBlockReplacerProvider() {
			@Override
			public IBiomeBlockReplacer create(ICubicWorld world, CubicBiome biome, BiomeBlockReplacerConfig conf) {
				return replacer;
			}

			@Override public Set<ConfigOptionInfo> getPossibleConfigOptions() {
				return Sets.newHashSet();
			}
		};
	}
}
