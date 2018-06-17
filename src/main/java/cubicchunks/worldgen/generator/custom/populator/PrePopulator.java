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
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrePopulator implements ICubicPopulator {

    private final CustomGeneratorSettings cfg;

    public PrePopulator(CustomGeneratorSettings cfg) {
        this.cfg = cfg;
    }

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome cubicBiome) {
        Biome biome = cubicBiome.getBiome();
        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && cfg.waterLakes && random.nextInt(cfg.waterLakeRarity) == 0) {
            (new WorldGenLakes(Blocks.WATER)).generate((World) world, random, pos.randomPopulationPos(random));
        }

        if (random.nextInt(cfg.lavaLakeRarity) == 0 && cfg.lavaLakes) {
            int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int blockY = pos.getMinBlockY() + yOffset;
            if (random.nextDouble() <= lavaLakeProbability(cfg, blockY)) {
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;

                if (blockY < cfg.waterLevel || random.nextInt(cfg.aboveSeaLavaLakeRarity) == 0) {
                    BlockPos blockPos = pos.getMinBlockPos().add(xOffset, yOffset, zOffset);
                    (new WorldGenLakes(Blocks.LAVA)).generate((World) world, random, blockPos);
                }
            }
        }

        if (cfg.dungeons) {
            for (int i = 0; i < cfg.dungeonCount; ++i) {
                (new WorldGenDungeons()).generate((World) world, random, pos.randomPopulationPos(random));
            }
        }

    }

    private double lavaLakeProbability(CustomGeneratorSettings cfg, int y) {
        // same as DefaultDecorator.waterSourceProbabilityForY
        final double yScale = -0.0242676003062542;
        final double yOffset = 0.723583275161355;
        final double valueScale = 0.00599930877922822;

        double normalizedY = (y - cfg.expectedBaseHeight) / cfg.expectedHeightVariation;
        double vanillaY = normalizedY * 64 + 64;
        return (Math.atan(vanillaY * yScale + yOffset) + Math.PI / 2) * valueScale;
    }
}
