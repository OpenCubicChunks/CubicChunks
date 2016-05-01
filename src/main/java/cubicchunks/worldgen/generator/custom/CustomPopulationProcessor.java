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

import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.generator.custom.features.BiomeFeatures;
import cubicchunks.worldgen.generator.custom.features.FeatureGenerator;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CustomPopulationProcessor extends CubeProcessor {

	private Map<BiomeGenBase, BiomeFeatures> biomeFeaturesMap;

	public CustomPopulationProcessor(String name, World world, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);

		this.biomeFeaturesMap = new HashMap<>();

		// for now use global for all biomes
		for (BiomeGenBase biome : BiomeGenBase.REGISTRY) {
			if(biome == null){
				continue;
			}
			this.biomeFeaturesMap.put(biome, new BiomeFeatures(world, biome));
		}
	}

	@Override
	public boolean calculate(Cube cube) {
		if(true)return true;
		WorldContext worldContext = WorldContext.get(cube.getWorld());
		if (!worldContext.cubeAndNeighborsExist(cube, true, GeneratorStage.POPULATION)) {
			return false;
		}

		BiomeGenBase biome = worldContext.getWorld().getBiomeGenForCoords(Coords.getCubeCenter(cube));
    
		//For surface generators we should actually use special RNG with seed 
		//that depends only in world seed and cube X/Z
		//but using this for surface generation doesn't cause any noticable issues
		Random rand = new Random(cube.cubeRandomSeed());
		
		BiomeFeatures features = this.biomeFeaturesMap.get(biome);
		for (FeatureGenerator gen : features.getBiomeFeatureGenerators()) {
			gen.generate(rand, cube, biome);
		}

		return true;
	}
}
