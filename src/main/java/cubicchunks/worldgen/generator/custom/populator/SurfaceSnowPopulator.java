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
package cubicchunks.worldgen.generator.custom.populator;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SurfaceSnowPopulator implements ICubicPopulator {

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome biome) {
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int xOffset = dx + Cube.SIZE / 2;
                int zOffset = dz + Cube.SIZE / 2;
                BlockPos aboveTop = PopulatorUtils.getSurfaceForCube(world, pos, xOffset, zOffset, 0, PopulatorUtils.SurfaceType.BLOCKING_MOVEMENT);
                if (aboveTop == null) {
                    continue;
                }
                BlockPos topBlock = aboveTop.down();

                if (world.canBlockFreezeWater(topBlock)) {
                    world.setBlockState(topBlock, Blocks.ICE.getDefaultState(), 2);
                }

                if (world.canSnowAt(aboveTop, true)) {
                    world.setBlockState(aboveTop, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }
            }
        }
    }
}
