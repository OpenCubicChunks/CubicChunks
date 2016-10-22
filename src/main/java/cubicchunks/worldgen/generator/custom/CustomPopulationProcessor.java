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
package cubicchunks.worldgen.generator.custom;

import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.features.BiomeFeatures;
import cubicchunks.worldgen.generator.custom.features.FeatureGenerator;

public class CustomPopulationProcessor {

	private Map<Biome, BiomeFeatures> biomeFeaturesMap;

	public CustomPopulationProcessor(ICubicWorld world) {
		this.biomeFeaturesMap = new HashMap<>();

		// for now use global for all biomes
		for (Biome biome : Biome.REGISTRY) {
			if (biome == null) {
				continue;
			}
			this.biomeFeaturesMap.put(biome, new BiomeFeatures(world, biome));
		}
	}

	public void populate(Cube cube) {
		Biome biome = cube.getCubicWorld().getBiome(Coords.getCubeCenter(cube));

		//For surface generators we should actually use special RNG with seed 
		//that depends only in world seed and cube X/Z
		//but using this for surface generation doesn't cause any noticable issues
		Random rand = new Random(cube.cubeRandomSeed());

		BiomeFeatures features = this.biomeFeaturesMap.get(biome);
		for (FeatureGenerator gen : features.getBiomeFeatureGenerators()) {
			gen.generate(rand, cube, biome);
		}
	}
}
