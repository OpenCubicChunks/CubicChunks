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

import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

import cubicchunks.world.ICubicWorld;

public class TallGrassGenerator extends SurfaceFeatureGenerator {

	private final IBlockState block;

	public TallGrassGenerator(final ICubicWorld world, final BlockTallGrass.EnumType tallGrassType) {
		super(world);

		this.block = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, tallGrassType);
	}

	@Override
	public void generateAt(final Random rand, final BlockPos pos, final Biome biome) {
		BlockPos currentPos = pos;

		for (int i = 0; i < 128; ++i) {
			BlockPos randomPos = currentPos.add(rand.nextInt(8) - rand.nextInt(8),
				rand.nextInt(4) - rand.nextInt(4),
				rand.nextInt(8) - rand.nextInt(8));

			if (world.isAirBlock(randomPos) && Blocks.TALLGRASS.canBlockStay((World) world, randomPos, block)) {
				this.setBlockOnly(randomPos, block);
			}
		}
	}
}
