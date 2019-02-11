/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * Implement this interface to your world generators and register them in
 * {@link CubeGeneratorsRegistry} to launch them
 * single time for each generated cube right after terrain and biome specific
 * generators.
 */
public interface ICubicPopulator {

    /**
     * Generate a specific populator feature for a given cube given a biome.
     *
     * To avoid requiring unnecessary amount of Cubes to do population,
     * the population space is offset by 8 blocks in each direction.
     * Instead of generating blocks only in this cube you should generate then in 16x16x16
     * block space starting from the center of current cube.
     *
     * Example of generating random position coordinate:
     *
     * {@code int x = random.nextInt(16) + 8 + pos.getXCenter();}
     *
     * You can also use {@link CubePos#randomPopulationPos} to generate random position in population space.
     *
     * All block access should be done through the provided {@link ICubicWorld} instance.
     *
     * @param random the cube specific {@link Random}.
     * @param pos is the position of the cube being populated {@link BlockPos}.
     * @param world The {@link ICubicWorld} we're generating for. Casting it to {@link World} is always safe.
     * @param biome The biome the populator is working in.
     *
     */
    void generate(World world, Random random, CubePos pos, Biome biome);
}
