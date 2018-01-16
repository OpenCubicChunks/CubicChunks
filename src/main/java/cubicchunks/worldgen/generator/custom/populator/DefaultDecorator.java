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

import static cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.genOreBellCurve;
import static cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.genOreUniform;
import static cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.getSurfaceForCube;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.CubePos;
import cubicchunks.util.MathUtil;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.SurfaceType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultDecorator implements ICubicPopulator {

    public static class Ores implements ICubicPopulator {

        @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome biome) {
            CustomGeneratorSettings cfg = CustomGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions());
            generateOres(world, cfg, random, pos);
        }

        private void generateOres(ICubicWorld world, CustomGeneratorSettings cfg, Random random, CubePos pos) {
            // TODO: allow interleaved order
            for (CustomGeneratorSettings.StandardOreConfig c : cfg.standardOres) {
                genOreUniform(world, cfg, random, pos, c.spawnTries, c.spawnProbability, new WorldGenMinable(c.blockstate, c.spawnSize),
                        c.minHeight, c.maxHeight);
            }
            for (CustomGeneratorSettings.PeriodicGaussianOreConfig c : cfg.periodicGaussianOres) {
                genOreBellCurve(world, cfg, random, pos, c.spawnTries, c.spawnProbability, new WorldGenMinable(c.blockstate, c.spawnSize),
                        c.heightMean, c.heightStdDeviation, c.heightSpacing, c.minHeight, c.maxHeight);
            }
        }
    }

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome biome) {
        CustomGeneratorSettings cfg = CustomGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions());

        // TODO: Biome decoration events?
        BiomeDecorator dec = biome.getBiome().decorator;
        generateOnTop(world, random, pos, dec.sandPatchesPerChunk, dec.sandGen);
        generateOnTop(world, random, pos, dec.clayPerChunk, dec.clayGen);
        generateOnTop(world, random, pos, dec.gravelPatchesPerChunk, dec.gravelGen);

        int treeCount = random.nextFloat() < dec.extraTreeChance ? dec.treesPerChunk + 1 : dec.treesPerChunk;
        for (int i = 0; i < treeCount; ++i) {
            int xOffset1 = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset1 = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            WorldGenAbstractTree treeGen = biome.getBiome().getRandomTreeFeature(random);
            treeGen.setDecorationDefaults();
            BlockPos top1 = getSurfaceForCube(world, pos, xOffset1, zOffset1, 0, SurfaceType.OPAQUE);
            if (top1 != null && treeGen.generate((World) world, random, top1)) {
                treeGen.generateSaplings((World) world, random, top1);
            }
        }

        for (int i = 0; i < dec.bigMushroomsPerChunk; ++i) {
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos top = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
            if (top != null) {
                dec.bigMushroomGen.generate((World) world, random, top);
            }
        }

        for (int i = 0; i < dec.flowersPerChunk; ++i) {
            // vanilla chooses random height between 0 and topBlock+32.
            // Assuming average height a bit less than the average of sea level and 128,
            // then it should succeed about one in 5+2=7 times for a give cube
            // TODO: Flower gen: figure out the probabilities and do it right
            if (random.nextInt(7) != 0) {
                continue;
            }
            BlockPos blockPos = pos.randomPopulationPos(random);
            BlockFlower.EnumFlowerType type = biome.getBiome().pickRandomFlower(random, blockPos);
            BlockFlower flowerBlock = type.getBlockType().getBlock();

            if (flowerBlock.getDefaultState().getMaterial() != Material.AIR) {
                dec.flowerGen.setGeneratedBlock(flowerBlock, type);
                dec.flowerGen.generate((World) world, random, blockPos);
            }
        }


        for (int i = 0; i < dec.grassPerChunk; ++i) {
            // vanilla chooses random height between 0 and topBlock*2.
            // Then the grass generator goes down to find the top block.
            // grass underground is quite rare so we can assume it almost never happens
            // and generate only at the "real" top. And it will happen on average half of the time.
            if (random.nextBoolean()) {
                continue;
            }
            // because vanilla grass generator goes down looking for a solid block
            // make sure there actually is one
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.SOLID);
            if (blockPos != null) {
                biome.getBiome().getRandomWorldGenForGrass(random).generate((World) world, random, blockPos);
            }
        }

        for (int i = 0; i < dec.deadBushPerChunk; ++i) {
            // same as above
            if (random.nextBoolean()) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.SOLID);
            if (blockPos != null) {
                (new WorldGenDeadBush()).generate((World) world, random, blockPos);
            }
        }

        for (int i = 0; i < dec.waterlilyPerChunk; ++i) {
            // same as above
            if (random.nextBoolean()) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos top = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
            if (top != null) {
                dec.waterlilyGen.generate((World) world, random, top);
            }
        }

        int mushroomCount = Math.max(dec.mushroomsPerChunk + 1, 1);
        for (int i = 0; i < mushroomCount; ++i) {
            if (random.nextInt(4) == 0) {
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos top = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
                if (top != null) {
                    dec.mushroomBrownGen.generate((World) world, random, top);
                }
            }

            if (random.nextInt(8) == 0) {
                // vanilla chooses random height between 0 and topBlock*2.
                // The WorldGenBush (unlike WorldGenDeadBush and grass generator)
                // won't go down to find the top block. It just attempts to generate at that position.
                // So assuming vanilla average terrain height is 5*16, it would generate for one attempt in cube
                // about one in 2*5=10 times
                if (random.nextInt(10) != 0) {
                    continue;
                }
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
                if (blockPos != null) {
                    dec.mushroomRedGen.generate((World) world, random, blockPos);
                }
            }
        }

        int reedCount = Math.max(dec.reedsPerChunk + 10, 10);
        for (int i = 0; i < reedCount; ++i) {
            // same as for red mushrooms above
            if (random.nextInt(10) != 0) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;

            BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
            if (blockPos != null) {
                dec.reedGen.generate((World) world, random, blockPos);
            }
        }

        // *10 - same reason as for red mushrooms
        if (random.nextInt(32 * 10) == 0) {
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;

            BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
            if (blockPos != null) {
                (new WorldGenPumpkin()).generate((World) world, random, blockPos);
            }
        }


        for (int i = 0; i < dec.cactiPerChunk; ++i) {
            // same as for red mushrooms above
            if (random.nextInt(10) != 0) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;

            BlockPos blockPos = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.OPAQUE);
            if (blockPos != null) {
                dec.cactusGen.generate((World) world, random, blockPos);
            }
        }


        if (dec.generateFalls) {
            for (int i = 0; i < 50; ++i) {
                int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                double prob = waterSourceProbabilityForY(cfg, pos.getMinBlockY() + yOffset);
                if (random.nextDouble() > prob) {
                    continue;
                }
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos blockPos = pos.getMinBlockPos().add(xOffset, yOffset, zOffset);
                (new WorldGenLiquids(Blocks.FLOWING_WATER)).generate((World) world, random, blockPos);
            }


            for (int i = 0; i < 20; ++i) {
                int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                double prob = lavaSourceProbabilityForY(cfg, pos.getMinBlockY() + yOffset);
                if (random.nextDouble() > prob) {
                    continue;
                }
                int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
                BlockPos blockPos = pos.getMinBlockPos().add(xOffset, yOffset, zOffset);
                (new WorldGenLiquids(Blocks.FLOWING_LAVA)).generate((World) world, random, blockPos);
            }

        }

    }

    private double waterSourceProbabilityForY(CustomGeneratorSettings cfg, int y) {
        // exact vanilla probability distribution here involves harmonic series,
        // so no nice formula and no generalization for negative heights
        // and logarithm based approximation would have infinity somewhere, so alo no ice generalization.
        // This is the best fit I found with arctan, which nicely extends for any height and seems to relatively nicely fit
        // found using libreoffice solver, minimizing sum of squares of differences between probabilities
        // estimated from 10,000,000 samples and function in form (arctan(x*yScale + yOffset) + pi/2)*valueScale
        // adding pi/2 instead of separate constant so that it never reaches zero or negative values.
        // Note in case it ever gets changed in vanilla: this was found for this random Y:
        // random.nextInt(random.nextInt(248) + 8);
        final double yScale = -0.0242676003062542;
        final double yOffset = 0.723583275161355;
        final double valueScale = 0.00599930877922822;

        double normalizedY = (y - cfg.heightOffset) / cfg.heightFactor;
        double vanillaY = normalizedY * 64 + 64;
        return (Math.atan(vanillaY * yScale + yOffset) + Math.PI / 2) * valueScale;
    }

    private double lavaSourceProbabilityForY(CustomGeneratorSettings cfg, int y) {
        // same as for water
        // Note in case it ever gets changed in vanilla: this was found for this random Y:
        // random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
        final double yScale = -0.0703727292987445;
        final double yOffset = 1.01588640105311;
        final double valueScale = 0.0127618337650875;

        double normalizedY = (y - cfg.heightOffset) / cfg.heightFactor;
        double vanillaY = normalizedY * 64 + 64;
        return (Math.atan(vanillaY * yScale + yOffset) + Math.PI / 2) * valueScale;
    }

    private void generateOnTop(ICubicWorld world, Random random, CubePos pos, int count, WorldGenerator generator) {
        for (int i = 0; i < count; ++i) {
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos top = getSurfaceForCube(world, pos, xOffset, zOffset, 0, SurfaceType.SOLID);
            if (top != null) {
                generator.generate((World) world, random, top);
            }
        }
    }


}
