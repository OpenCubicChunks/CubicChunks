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
package cubicchunks.debug;

import com.flowpowered.noise.module.source.Perlin;
import cubicchunks.CubicChunks;
import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.ICubePrimer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DebugWorldType extends WorldType implements ICubicWorldType {

    private DebugWorldType() {
        super("DebugCubic");
    }

    @Override
    public boolean canBeCreated() {
        return CubicChunks.DEBUG_ENABLED;
    }

    public static void create() {
        new DebugWorldType();
    }

    @Override
    public WorldProvider getReplacedProviderFor(WorldProvider provider) {
        return provider;
    }

    @Override public ICubeGenerator createCubeGenerator(ICubicWorld world) {
        //TODO: move first light processor directly into cube?
        return new BasicCubeGenerator(world) {
            @Nonnull Perlin perlin = new Perlin();

            {
                perlin.setFrequency(0.180);
                perlin.setOctaveCount(1);
                perlin.setSeed((int) world.getSeed());
            }

            //TODO: find out what this was/should have been for (it was never used)
            //CustomPopulationProcessor populator = new CustomPopulationProcessor(world);

            @Override public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
                ICubePrimer primer = new CubePrimer();

                if (cubeY > 30) {
                    return primer;
                }
                if (cubeX == 100 && cubeZ == 100) {
                    return primer; //hole in the world
                }
                CubePos cubePos = new CubePos(cubeX, cubeY, cubeZ);
                for (BlockPos pos : BlockPos.getAllInBoxMutable(cubePos.getMinBlockPos(), cubePos.getMaxBlockPos())) {
                    double currDensity = perlin.getValue(pos.getX(), pos.getY() * 0.5, pos.getZ());
                    double aboveDensity = perlin.getValue(pos.getX(), (pos.getY() + 1) * 0.5, pos.getZ());
                    if (cubeY >= 16) {
                        currDensity -= (pos.getY() - 16 * 16) / 100;
                        aboveDensity -= (pos.getY() + 1 - 16 * 16) / 100;
                    }
                    if (currDensity > 0.5) {
                        if (currDensity > 0.5 && aboveDensity <= 0.5) {
                            primer.setBlockState(Coords.blockToLocal(pos.getX()), Coords.blockToLocal(pos.getY()), Coords.blockToLocal(pos.getZ()),
                                    Blocks.GRASS.getDefaultState());
                        } else if (currDensity > aboveDensity && currDensity < 0.7) {
                            primer.setBlockState(Coords.blockToLocal(pos.getX()), Coords.blockToLocal(pos.getY()), Coords.blockToLocal(pos.getZ()),
                                    Blocks.DIRT.getDefaultState());
                        } else {
                            primer.setBlockState(Coords.blockToLocal(pos.getX()), Coords.blockToLocal(pos.getY()), Coords.blockToLocal(pos.getZ()),
                                    Blocks.STONE.getDefaultState());
                        }
                    }
                }
                return primer;
            }

            @Override public void populate(Cube cube) {
                //populator.calculate(cube);
            }

            @Override
            public Box getPopulationRequirement(Cube cube) {
                return NO_POPULATOR_REQUIREMENT;
            }
        };
    }
}
