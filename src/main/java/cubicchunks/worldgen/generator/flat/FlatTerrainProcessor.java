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
package cubicchunks.worldgen.generator.flat;

import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubeGeneratorsRegistry;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A cube generator that generates a flat surface of grass, dirt and stone.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FlatTerrainProcessor extends BasicCubeGenerator {

    public FlatTerrainProcessor(ICubicWorld world) {
        super(world);
    }

    @Override
    public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        ICubePrimer primer = new CubePrimer();

        if (cubeY >= 0) {
            return primer;
        }
        if (cubeY == -1) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    primer.setBlockState(x, 15, z, Blocks.GRASS.getDefaultState());
                    for (int y = 14; y >= 10; y--) {
                        primer.setBlockState(x, y, z, Blocks.DIRT.getDefaultState());
                    }
                    for (int y = 9; y >= 0; y--) {
                        primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
                    }
                }
            }
            return primer;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    IBlockState state = Blocks.STONE.getDefaultState();
                    if (Coords.localToBlock(cubeY, y) == world.getMinHeight()) {
                        state = Blocks.BEDROCK.getDefaultState();
                    }
                    primer.setBlockState(x, y, z, state);
                }
            }
        }

        return primer;
    }

    @Override
    public void populate(Cube cube) {
        CubeGeneratorsRegistry.generateWorld(new Random(cube.cubeRandomSeed()), cube.getCoords().getMinBlockPos(), (World) world);
    }

    @Override
    public Box getPopulationRequirement(Cube cube) {
        return NO_POPULATOR_REQUIREMENT;
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean flag) {
        return name.equals("Stronghold") ? new BlockPos(0, 0, 0) : null; // eyes of ender are the new F3 for finding the origin :P
    }
}
