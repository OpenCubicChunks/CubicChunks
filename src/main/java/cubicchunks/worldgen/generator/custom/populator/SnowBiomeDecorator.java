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

import static cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.getSurfaceForCube;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeSnow;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SnowBiomeDecorator implements ICubicPopulator {

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome biome) {
        BiomeSnow snow = (BiomeSnow) biome.getBiome();
        if (snow.superIcy) {
            for (int i = 0; i < 3; ++i) {
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, PopulatorUtils.SurfaceType.SOLID);
                if (blockPos != null) {
                    snow.iceSpike.generate((World) world, random, blockPos);
                }
            }

            for (int l = 0; l < 2; ++l) {
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, PopulatorUtils.SurfaceType.SOLID);
                if (blockPos != null) {
                    snow.icePatch.generate((World) world, random, blockPos);
                }
            }
        }
    }
}
