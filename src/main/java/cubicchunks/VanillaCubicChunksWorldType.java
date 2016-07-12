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
package cubicchunks;

import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.DependentGeneratorStage;
import cubicchunks.worldgen.GeneratorStageRegistry;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.dependency.RegionDependency;
import cubicchunks.worldgen.generator.vanilla.VanillaPopulationProcessor;
import cubicchunks.worldgen.generator.vanilla.VanillaTerrainProcessor;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderOverworld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VanillaCubicChunksWorldType extends BaseCubicWorldType {

	public VanillaCubicChunksWorldType() {
		super("VanillaCubic");
	}

	@Override public void registerWorldGen(ICubicWorldServer world, GeneratorStageRegistry generatorStageRegistry) {
		ChunkProviderOverworld vanillaGen = new ChunkProviderOverworld((World) world, world.getSeed(), true, "");

		// init the world's GeneratorStageRegistry
		GeneratorStage terrain = new VanillaStage("terrain", new Vec3i(0, 0, 0), new Vec3i(0, 0, 0));
		DependentGeneratorStage lighting = new DependentGeneratorStage("lighting", null);
		lighting.setCubeDependency(new RegionDependency(lighting, 2));
		GeneratorStage population = new VanillaStage("population", new Vec3i(0, 0, 0), new Vec3i(1, 0, 1));

		generatorStageRegistry.addStage(terrain, new VanillaTerrainProcessor(world, vanillaGen));
		generatorStageRegistry.addStage(lighting, new FirstLightProcessor(lighting, world));
		generatorStageRegistry.addStage(population, new VanillaPopulationProcessor(vanillaGen));
	}

	public static void create() {
		new VanillaCubicChunksWorldType();
	}

	private static class VanillaStage extends GeneratorStage {
		private final CubeDependency[] depsForHeight = new CubeDependency[16];

		private VanillaStage(String name, Vec3i depStart, Vec3i depEnd) {
			super(name);
			//all cubes from y=1 to y=15 depend on cube at y=0, which generates all of them
			for (int y = 1; y < 16; y++) {
				int startY = depStart.getY() - y;
				int endY = startY;
				Vec3i start = new Vec3i(depStart.getX(), startY, depStart.getZ());
				Vec3i end = new Vec3i(depEnd.getX(), endY, depEnd.getZ());
				CubeDependency dep = new RegionDependency(this, start, end);
				depsForHeight[y] = dep;
			}
			//cube at y=0 depends on all other so that they are loaded
			depsForHeight[0] = new RegionDependency(this, new Vec3i(0, 1, 0), new Vec3i(0, 15, 0));
		}

		@Nullable @Override public CubeDependency getCubeDependency(@Nonnull Cube cube) {
			if (cube.getY() < 0 || cube.getY() >= depsForHeight.length) {
				return null;
			}
			return depsForHeight[cube.getY()];
		}
	}
}
