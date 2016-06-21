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
import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;
import net.minecraft.world.gen.ChunkProviderOverworld;

import java.util.HashSet;
import java.util.Set;

public class VanillaPopulationProcessor extends CubeProcessor {
	
	private GeneratorStage generatorStage;
	private ServerCubeCache provider;
	private ICubicWorldServer world;
	private ChunkProviderOverworld vanillaGen;

	public VanillaPopulationProcessor(GeneratorStage generatorStage, ICubicWorldServer world, ChunkProviderOverworld vanillaGen, int batchSize) {
		super("Population", world.getCubeCache(), batchSize);
		this.generatorStage = generatorStage;
		this.provider = world.getCubeCache();
		this.world = world;
		this.vanillaGen = vanillaGen;
	}

	@Override
	public Set<Cube> calculate(Cube cube) {
		if (cube.getY() != 0) {
			return Sets.newHashSet(cube);
		}
		Set<Cube> cubes = new HashSet<>();
		for (int cubeY = 0; cubeY < 16; cubeY++) {
			Cube currentCube = this.provider.getCube(cube.getX(), cubeY, cube.getZ());
			assert currentCube != null;
			cubes.add(currentCube);
		}
		try {
			world.setGeneratingWorld(true);
			this.vanillaGen.populate(cube.getX(), cube.getZ());
		} catch (RuntimeException ex) {
			CubicChunks.LOGGER.error("Exception when populating chunk at " + cube.getX() + ", " + cube.getZ(), ex);
		} finally {
			world.setGeneratingWorld(false);
		}
		return cubes;
	}
}
