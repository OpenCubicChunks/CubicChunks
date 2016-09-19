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
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.ICubicChunkGenerator;
import cubicchunks.worldgen.generator.flat.FlatTerrainProcessor;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class DebugWorldType extends BaseCubicWorldType {

	public DebugWorldType() {
		super("DebugCubic");
	}

	@Override
	public boolean getCanBeCreated() {
		return System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
	}

	public static void create() {
		new DebugWorldType();
	}

	@Override public ICubicChunkGenerator createCubeGenerator(ICubicWorldServer world) {
		FlatTerrainProcessor gen =  new FlatTerrainProcessor();
		//TODO: move first light processor directly into cube?
		FirstLightProcessor light = new FirstLightProcessor(world);
		return new ICubicChunkGenerator() {
			@Override public void generateCube(Cube cube) {
				gen.calculate(cube);
			}

			@Override public void populateCube(Cube cube) {
				ICubicWorld world = cube.getWorld();
				cube.setBlockForGeneration(new BlockPos(8, 8, 8), Blocks.DIAMOND_BLOCK.getDefaultState());
				for (int dx = 0; dx <= 1; dx++) {
					for (int dy = 0; dy <= 1; dy++) {
						for (int dz = 0; dz <= 1; dz++) {
							Cube cubeOffset = world.getCubeCache().getCube(cube.getX() + dx, cube.getY() + dy, cube.getZ() + dz);
							if (cubeOffset == null) {
								throw new RuntimeException("Generating cube at " + cube.getCoords() + " - cube at " +
										new CubeCoords(cube.getX() + dx, cube.getY() + dy, cube.getZ() + dz) +
										" doesn't exist");
							}
						}
					}
				}
			}
		};
	}
}