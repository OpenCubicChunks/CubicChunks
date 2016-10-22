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
import net.minecraft.world.biome.Biome;

import java.util.Random;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;

public abstract class FeatureGenerator {
	protected final ICubicWorld world;

	public FeatureGenerator(final ICubicWorld world) {
		this.world = world;
	}

	public abstract void generate(final Random rand, final Cube cube, final Biome biome);

	protected boolean setBlockOnly(final BlockPos blockPos, final IBlockState blockState) {
		return this.world.setBlockState(blockPos, blockState, 2);
	}

	protected boolean setBlockAndUpdateNeighbors(final BlockPos pos, final IBlockState state) {
		return this.world.setBlockState(pos, state, 3);
	}

	protected IBlockState getBlockState(final BlockPos pos) {
		return this.world.getBlockState(pos);
	}

	protected static int getMinCubeY(final int y) {
		return (y >> 4) << 4;
	}
}
