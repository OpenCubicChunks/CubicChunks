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
package cubicchunks.worldgen.generator.vanilla;

import com.google.common.collect.Sets;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;
import net.minecraft.world.gen.ChunkProviderOverworld;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VanillaPopulationProcessor extends CubeProcessor {
	private ICubeCache provider;
	private ChunkProviderOverworld vanillaGen;

	public VanillaPopulationProcessor(ICubeCache provider, ChunkProviderOverworld vanillaGen, int batchSize) {
		super("Population", provider, batchSize);
		this.provider = provider;
		this.vanillaGen = vanillaGen;
	}

	@Override
	public Set<Cube> calculate(Cube cube) {
		if (cube.getY() < 0 || cube.getY() >= 16) {
			return Sets.newHashSet(cube);
		}
		if(!canPopulate(cube)) {
			return Collections.EMPTY_SET;
		}
		Set<Cube> cubes = new HashSet<>();
		for (int cubeY = 0; cubeY < 16; cubeY++) {
			Cube currentCube = this.provider.getCube(cube.getX(), cubeY, cube.getZ());
			if (currentCube == null) {
				throw new IllegalStateException();
			}
			cubes.add(currentCube);
		}
		this.vanillaGen.populate(cube.getX(), cube.getZ());
		return cubes;
	}

	private boolean canPopulate(Cube c00) {
		Cube c01 = provider.getCube(c00.getX(), c00.getY(), c00.getZ() + 1);
		Cube c11 = provider.getCube(c00.getX() + 1, c00.getY(), c00.getZ() + 1);
		Cube c10 = provider.getCube(c00.getX() + 1, c00.getY(), c00.getZ());
		return !c01.getGeneratorStage().isLessThan(GeneratorStage.POPULATION) &&
				!c11.getGeneratorStage().isLessThan(GeneratorStage.POPULATION) &&
				!c10.getGeneratorStage().isLessThan(GeneratorStage.POPULATION);
	}
}
