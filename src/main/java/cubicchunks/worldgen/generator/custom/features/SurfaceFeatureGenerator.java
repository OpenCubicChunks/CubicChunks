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
package cubicchunks.worldgen.generator.custom.features;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;

public abstract class SurfaceFeatureGenerator extends FeatureGenerator {

	public SurfaceFeatureGenerator(ICubicWorld world) {
		super(world);
	}

	@Override
	public void generate(Random rand, Cube cube, Biome biome) {
		BlockPos cubeCenter = Coords.getCubeCenter(cube);

		int x = cubeCenter.getX() + rand.nextInt(16);
		int z = cubeCenter.getZ() + rand.nextInt(16);

		int y = cubeCenter.getY() + 16;
		int minY = cubeCenter.getY();

		BlockPos pos = new BlockPos(x, y, z);

		boolean foundSurface = false;
		while (pos.getY() >= minY) {
			if (isSurfaceAt(pos)) {
				foundSurface = true;
				break;
			}
			pos = pos.down();
		}
		// next attempt. We didn't find place to generate it
		if (foundSurface) {
			this.generateAt(rand, pos, biome);
		}
	}

	protected boolean isSurfaceAt(BlockPos pos) {
		//we don't really know if it's the top block.
		//assume it's surface if there is solid block with air above it
		IBlockState stateBelow = getBlockState(pos.down());
		IBlockState state = getBlockState(pos);

		return stateBelow.isOpaqueCube() && state.getBlock().isAir(state, (World) world, pos);
	}

	/**
	 * Generates feature at given position.
	 *
	 * @param rand RNG to use
	 * @param pos position of air block with solid block below it
	 */
	public abstract void generateAt(Random rand, BlockPos pos, Biome biome);
}
