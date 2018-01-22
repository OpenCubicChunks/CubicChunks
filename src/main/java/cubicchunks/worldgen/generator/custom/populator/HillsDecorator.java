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

import static cubicchunks.worldgen.generator.custom.populator.PopulatorUtils.genOreUniform;
import static net.minecraft.block.state.pattern.BlockMatcher.forBlock;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeHills;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HillsDecorator implements ICubicPopulator {

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome biome) {
        // TODO: Find less awful way to do it
        CustomGeneratorSettings cfg = CustomGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions());
/*
        genOreUniform(world, cfg, random, pos, cfg.hillsEmeraldOreSpawnTries, cfg.hillsEmeraldOreSpawnProbability,
                new WorldGenEmerald(), cfg.hillsEmeraldOreSpawnMinHeight, cfg.hillsEmeraldOreSpawnMaxHeight);

        WorldGenerator silverfishStoneGen = new WorldGenMinable(Blocks.MONSTER_EGG.getDefaultState()
                .withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE), cfg.hillsSilverfishStoneSpawnSize);
        genOreUniform(world, cfg, random, pos, cfg.hillsSilverfishStoneSpawnTries, cfg.hillsSilverfishStoneSpawnProbability,
                silverfishStoneGen, cfg.hillsSilverfishStoneSpawnMinHeight, cfg.hillsSilverfishStoneSpawnMaxHeight);*/
    }

    private static class WorldGenEmerald extends WorldGenerator {

        @Override public boolean generate(World worldIn, Random rand, BlockPos position) {
            position = position.add(8, 0, 8); // because PopulatorUtils.genOre* expects this (because vanilla does this)
            IBlockState state = worldIn.getBlockState(position);
            if (state.getBlock().isReplaceableOreGen(state, worldIn, position, forBlock(Blocks.STONE))) {
                worldIn.setBlockState(position, Blocks.EMERALD_ORE.getDefaultState(), 2);
            }
            return true;
        }
    }
}
