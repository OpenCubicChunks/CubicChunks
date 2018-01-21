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

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.localToBlock;

import cubicchunks.util.CubePos;
import cubicchunks.util.MathUtil;
import cubicchunks.world.CubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PopulatorUtils {

    /**
     * Returns the minimum coordinate inside the population area this coordinate is in
     */
    public static int getMinCubePopulationPos(int coord) {
        return localToBlock(blockToCube(coord), Cube.SIZE / 2);
    }

    public static void genOreUniform(CubicWorld world, CustomGeneratorSettings cfg, Random random, CubePos pos,
                                     int count, double probability, WorldGenerator generator, double minY, double maxY) {
        int minBlockY = Math.round((float) (minY * cfg.heightFactor + cfg.heightOffset));
        int maxBlockY = Math.round((float) (maxY * cfg.heightFactor + cfg.heightOffset));
        if (pos.getMinBlockY() > maxBlockY || pos.getMaxBlockY() < minBlockY) {
            return;
        }
        for (int i = 0; i < count; ++i) {
            if (random.nextDouble() > probability) {
                continue;
            }
            int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int blockY = pos.getMinBlockY() + yOffset;
            if (blockY > maxBlockY || blockY < minBlockY) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE);
            int zOffset = random.nextInt(Cube.SIZE);
            generator.generate((World) world, random, new BlockPos(pos.getMinBlockX() + xOffset, blockY, pos.getMinBlockZ() + zOffset));
        }
    }

    public static void genOreBellCurve(CubicWorld world, CustomGeneratorSettings cfg, Random random, CubePos pos, int count,
                                       double probability, WorldGenerator generator, double mean, double stdDevFactor, double spacing, double minY, double maxY) {

        int factor = (cfg.getMaxHeight() - cfg.getMinHeight()) / 2;
        int minBlockY = Math.round((float) (minY * factor + cfg.heightOffset));
        int maxBlockY = Math.round((float) (maxY * factor+ cfg.heightOffset));
        //temporary fix for slider becoming 0 at minimum position
        if(spacing == 0.0){
            spacing = 0.5;
        }
        int iSpacing = Math.round((float) (spacing * factor));
        int iMean = Math.round((float) (mean * factor + cfg.heightOffset));
		double scaledStdDev = stdDevFactor * factor;
        for (int i = 0; i < count; ++i) {
            int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int blockY = pos.getMinBlockY() + yOffset;
            //skip all potential spawns outside the spawn range
            if((blockY > maxBlockY) || (blockY < minBlockY)){
                continue;
            }
            double modifier = MathUtil.bellCurveProbabilityCyclic(blockY, iMean, scaledStdDev, iSpacing);
            //Modify base probability with the curve
            if (random.nextDouble() > (probability * modifier)) {
                continue;
            }
            int xOffset = random.nextInt(Cube.SIZE);
            int zOffset = random.nextInt(Cube.SIZE);
            generator.generate((World) world, random, new BlockPos(pos.getMinBlockX() + xOffset, blockY, pos.getMinBlockZ() + zOffset));
        }
    }

    /**
     * Finds the top block for that population cube with give offset, or null if no suitable place found.
     * This method starts from the top of population area (or forcedAdditionalCubes*16 blocks above that)
     * and goes down scanning for solid block. The value is used only if it's within population area.
     *
     * Note: forcedAdditionalCubes should be zero unless absolutely necessary.
     * TODO: make it go up instead of down so it doesn't load unnecessary chunks when forcedAdditionalCubes is nonzero
     */
    @Nullable
    public static BlockPos getSurfaceForCube(CubicWorld world, CubePos pos, int xOffset, int zOffset, int forcedAdditionalCubes, SurfaceType type) {
        int maxFreeY = pos.getMaxBlockY() + Cube.SIZE / 2;
        int minFreeY = pos.getMinBlockY() + Cube.SIZE / 2;
        int startY = pos.above().getMaxBlockY() + forcedAdditionalCubes * Cube.SIZE;

        BlockPos start = new BlockPos(
                pos.getMinBlockX() + xOffset,
                startY,
                pos.getMinBlockZ() + zOffset
        );
        return findTopBlock(world, start, minFreeY, maxFreeY, type);
    }

    @Nullable
    public static BlockPos findTopBlock(CubicWorld world, BlockPos start, int minTopY, int maxTopY, SurfaceType type) {
        BlockPos pos = start;
        IBlockState startState = world.getBlockState(start);
        if (canBeTopBlock((World) world, pos, startState, type)) {
            // the top tested block is solid, don't use that one
            return null;
        }
        while (pos.getY() >= minTopY) {
            BlockPos next = pos.down();
            IBlockState state = world.getBlockState(next);
            if (canBeTopBlock((World) world, pos, state, type)) {
                break;
            }
            pos = next;
        }
        if (pos.getY() < minTopY || pos.getY() > maxTopY) {
            return null;
        }
        return pos;
    }

    public static boolean canBeTopBlock(World world, BlockPos pos, IBlockState state, SurfaceType type) {
        if (type == SurfaceType.SOLID) {
            return state.getMaterial().blocksMovement()
                    && !state.getBlock().isLeaves(state, world, pos)
                    && !state.getBlock().isFoliage(world, pos);
        } else if (type == SurfaceType.OPAQUE) {
            return state.getLightOpacity(world, pos) != 0;
        } else {
            return state.getMaterial().blocksMovement() || state.getMaterial().isLiquid();
        }
    }

    public enum SurfaceType {
        SOLID, BLOCKING_MOVEMENT, OPAQUE
    }
}
