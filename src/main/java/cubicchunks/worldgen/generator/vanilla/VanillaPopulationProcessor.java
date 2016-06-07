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
import cubicchunks.debug.Prof;
import net.minecraft.world.gen.ChunkProviderOverworld;

import java.util.Collections;
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
		if (cube.getY() < 0 || cube.getY() >= 16) {
			return Sets.newHashSet(cube);
		}
		if (!canPopulate(cube)) {
			return Collections.EMPTY_SET;
		}
		Prof.call("VanillaPopulationProcessor#calculate(Cube)[s]");
		Set<Cube> cubes = new HashSet<>();
		for (int cubeY = 0; cubeY < 16; cubeY++) {
			Cube currentCube = this.provider.forceLoadCube(cube, cube.getX(), cubeY, cube.getZ());
			this.provider.forceLoadCube(cube, cube.getX() + 1, cubeY, cube.getZ());
			this.provider.forceLoadCube(cube, cube.getX(), cubeY, cube.getZ() + 1);
			this.provider.forceLoadCube(cube, cube.getX() + 1, cubeY, cube.getZ() + 1);
			if (currentCube == null) {
				throw new IllegalStateException();
			}
			cubes.add(currentCube);
		}
		try {
			Prof.call("ChunkProviderOverworld#populate(Cube)");
			this.vanillaGen.populate(cube.getX(), cube.getZ());
		} catch (RuntimeException ex) {
			CubicChunks.LOGGER.error("Exception when populating chunk at " + cube.getX() + ", " + cube.getZ(), ex);
		}
		return cubes;
	}

	private boolean canPopulate(Cube c00) {
		Prof.call("VanillaPopulationProcessor#canPopulate(Cube)");
		Cube c01 = provider.getCube(c00.getX(), c00.getY(), c00.getZ() + 1);
		Cube c11 = provider.getCube(c00.getX() + 1, c00.getY(), c00.getZ() + 1);
		Cube c10 = provider.getCube(c00.getX() + 1, c00.getY(), c00.getZ());
		return c01 != null && !c01.getCurrentStage().precedes(generatorStage) &&
				c11 != null && !c11.getCurrentStage().precedes(generatorStage) &&
				c10 != null && !c10.getCurrentStage().precedes(generatorStage);
	}
}
