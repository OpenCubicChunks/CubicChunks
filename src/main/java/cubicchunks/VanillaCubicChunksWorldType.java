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

import cubicchunks.server.ServerCubeCache;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.GeneratorPipeline;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.dependency.RegionDependency;
import cubicchunks.worldgen.generator.vanilla.VanillaFirstLightProcessor;
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

	@Override public void registerWorldGen(ICubicWorldServer world, GeneratorPipeline pipeline) {
		ServerCubeCache cubeCache = world.getCubeCache();

		ChunkProviderOverworld vanillaGen = new ChunkProviderOverworld((World) world, world.getSeed(), true, "");

		// init the worldgen pipeline
		GeneratorStage terrain = new VanillaStage("terrain", new Vec3i(0, 0, 0), new Vec3i(0, 15, 0), null, null);
		GeneratorStage lighting = new VanillaStage("lighting", new Vec3i(-2, -2, -2), new Vec3i(2, 15 + 2, 2),
				new Vec3i(-2, -2, -2), new Vec3i(2, 2, 2));
		GeneratorStage population = new VanillaStage("population", new Vec3i(0, 0, 0), new Vec3i(1, 15, 1), null, null);

		pipeline.addStage(terrain, new VanillaTerrainProcessor(lighting, world, vanillaGen, 5));
		pipeline.addStage(lighting, new VanillaFirstLightProcessor(lighting, population, cubeCache, 5));
		pipeline.addStage(population, new VanillaPopulationProcessor(population, world, vanillaGen, 5));

	}

	public static void create() {
		new VanillaCubicChunksWorldType();
	}

	private static class VanillaStage extends GeneratorStage {
		private final CubeDependency[] depsForHeight = new CubeDependency[16];
		private CubeDependency normal;

		private VanillaStage(String name, Vec3i depStart, Vec3i depEnd, @Nullable Vec3i normalDepStart, @Nullable Vec3i normalDepEnd) {
			super(name);
			if (normalDepEnd != null && normalDepStart != null) {
				this.normal = new RegionDependency(this, normalDepStart, normalDepEnd);
			}
			for (int y = 0; y < 16; y++) {
				//each cube requires cubes from y=0 to y=15
				//but relative to them, these positions are different
				//relative coords: requiredCubePos - currentCubePos
				int startY = depStart.getY() - y;
				int endY = depEnd.getY() - y;
				Vec3i start = new Vec3i(depStart.getX(), startY, depStart.getZ());
				Vec3i end = new Vec3i(depEnd.getX(), endY, depEnd.getZ());
				CubeDependency dep = new RegionDependency(this, start, end);
				depsForHeight[y] = dep;
			}
		}

		@Nullable @Override public CubeDependency getCubeDependency(@Nonnull Cube cube) {
			if (cube.getY() < 0 || cube.getY() >= depsForHeight.length) {
				return normal;
			}
			return depsForHeight[cube.getY()];
		}
	}
}
