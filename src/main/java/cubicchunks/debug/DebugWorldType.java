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
package cubicchunks.debug;

import cubicchunks.BaseCubicWorldType;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.GeneratorPipeline;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.IndependentGeneratorStage;
import cubicchunks.worldgen.dependency.RegionDependency;
import cubicchunks.worldgen.generator.flat.FlatTerrainProcessor;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class DebugWorldType extends BaseCubicWorldType {

	public DebugWorldType() {
		super("DebugCubic");
	}

	@Override public void registerWorldGen(ICubicWorldServer world, GeneratorPipeline pipeline) {
		ServerCubeCache cubeCache = world.getCubeCache();
		// init the worldgen pipeline
		GeneratorStage terrain = new IndependentGeneratorStage("debug");
		GeneratorStage randomblock = new GeneratorStage("debug_randomblock") {
			@Override public CubeDependency getCubeDependency(Cube cube) {
				return new RegionDependency(this, 1);
			}
		};

		pipeline.addStage(terrain, new FlatTerrainProcessor());
		pipeline.addStage(randomblock, cube -> {
			cube.setBlockForGeneration(new BlockPos(0, 0, 0), Blocks.DIAMOND_BLOCK.getDefaultState());
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						Cube cubeOffset = cubeCache.getCube(cube.getX() + dx, cube.getY() + dy, cube.getZ() + dz);
						if (cubeOffset == null) {
							throw new RuntimeException("Generating cube at " + cube.getCoords() + " - cube at " +
									new CubeCoords(cube.getX() + dx, cube.getY() + dy, cube.getZ() + dz) +
									" doesn't exist");
						}
					}
				}
			}
		});
	}

	@Override
	public boolean getCanBeCreated() {
		return System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
	}

	public static void create() {
		new DebugWorldType();
	}
}