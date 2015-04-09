/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.generator;

import cubicchunks.generator.features.BiomeFeatures;
import cubicchunks.generator.features.FeatureGenerator;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class FeatureProcessor extends CubeProcessor {

	private Map<Biome, BiomeFeatures> biomeFeaturesMap;

	public FeatureProcessor(String name, World world, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);

		this.biomeFeaturesMap = new HashMap<Biome, BiomeFeatures>();

		BiomeFeatures global = new BiomeFeatures(world);

		// for now use global for all biomes
		for (Biome biome : Biome.getBiomeArray()) {
			this.biomeFeaturesMap.put(biome, global);
		}
	}

	@Override
	public boolean calculate(Cube cube) {
		WorldContext worldContext = WorldContext.get(cube.getWorld());
		if (!worldContext.cubeAndNeighborsExist(cube, true, GeneratorStage.FEATURES)) {
			return false;
		}

		Biome biome = worldContext.getWorld().getBiomeAt(Coords.getCubeCenter(cube));
    
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
