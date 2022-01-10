/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMaxBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;

import io.github.opencubicchunks.cubicchunks.api.util.MathUtil;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Notes on world.checkLightFor(): Decreasing light value: Light is recalculated starting from 0 ONLY for blocks where
 * rawLightValue is equal to savedLightValue (ie. updating skylight source that is not there anymore). Otherwise
 * existing light values are assumed to be correct. Generates and updates cube initial lighting, and propagates light
 * changes caused by generating cube downwards.
 * <p>
 * Used only when changes are caused by pre-populator terrain generation.
 * <p>
 * THIS SHOULD ONLY EVER BE USED ONCE PER CUBE.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FirstLightProcessor {

    private final MutableBlockPos mutablePos = new MutableBlockPos();

    /**
     * Diffuses skylight in the given cube and all cubes affected by this update.
     *
     * @param cube the cube whose skylight is to be initialized
     */
    public void diffuseSkylight(ICube cube) {
        ILightingManager lm = ((ICubicWorldInternal) cube.getWorld()).getLightingManager();
        Iterable<? extends BlockPos.MutableBlockPos> allBlocks = BlockPos.getAllInBoxMutable(
                cube.getCoords().getMinBlockPos(),
                cube.getCoords().getMaxBlockPos()
        );
        for (BlockPos.MutableBlockPos pos : allBlocks) {
            if (cube.getBlockState(pos).getLightValue(cube.getWorld(), pos) > 0) {
                lm.checkLightFor(EnumSkyBlock.BLOCK, pos);
            }
        }
        if (!cube.getWorld().provider.hasSkyLight()) {
            return;
        }

        // Cache min/max Y, generating them may be expensive
        int[][] minBlockYArr = new int[Cube.SIZE][Cube.SIZE];
        int[][] maxBlockYArr = new int[Cube.SIZE][Cube.SIZE];

        int minBlockX = cubeToMinBlock(cube.getX());
        int maxBlockX = cubeToMaxBlock(cube.getX());

        int minBlockZ = cubeToMinBlock(cube.getZ());
        int maxBlockZ = cubeToMaxBlock(cube.getZ());

        // the lowest minHeight and the highest maxHeight values
        // used to make the cube iteration the outer loop, so light propagator can do mass light updates
        int minMinHeight = Integer.MAX_VALUE;
        int maxMaxHeight = Integer.MIN_VALUE;

        // Determine the block columns that require updating. If there is nothing to update, store contradicting data so
        // we can skip the column later.

        IColumnInternal column = cube.getColumn();
        for (int localX = 0; localX < Cube.SIZE; ++localX) {
            for (int localZ = 0; localZ < Cube.SIZE; ++localZ) {
                int height = column.getHeightWithStaging(localX, localZ) - 1;
                int maxY = cube.getCoords().getMaxBlockY();
                int maxCubeBlockY = cube.getCoords().getMaxBlockY();

                int minCubeBlockX = cube.getCoords().getMinBlockX();
                int minCubeBlockZ = cube.getCoords().getMinBlockZ();
                mutablePos.setPos(minCubeBlockX + localX, 0, minCubeBlockZ + localZ);

                int minInstantFill = Integer.MIN_VALUE, maxInstantFill = Integer.MIN_VALUE;
                if (cube.getStorage() != null && localX != 0 && localX != 15 && localZ != 0 && localZ != 15 && maxCubeBlockY > height) {
                    int h1 = column.getHeightWithStaging(localX + 1, localZ);
                    int h2 = column.getHeightWithStaging(localX - 1, localZ);
                    int h3 = column.getHeightWithStaging(localX, localZ + 1);
                    int h4 = column.getHeightWithStaging(localX, localZ - 1);
                    int maxNeighbor = MathUtil.max(h1, h2, h3, h4);
                    int maxCurr = height + 2;
                    minInstantFill = MathUtil.max(maxCurr, maxNeighbor, cube.getCoords().getMinBlockY());
                    maxInstantFill = maxCubeBlockY;
                }
                if (height < maxY) {
                    int minCubeBlockY = cube.getCoords().getMinBlockY();
                    int minY = Math.max(height, minCubeBlockY);
                    for (int yPos = minY; yPos <= maxY; yPos++) {
                        mutablePos.setY(yPos);
                        if (yPos >= minInstantFill && yPos <= maxInstantFill) {
                            cube.setLightFor(EnumSkyBlock.SKY, mutablePos, 15);
                        } else {
                            lm.checkLightFor(EnumSkyBlock.SKY, mutablePos);
                        }
                    }
                }

                Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cube, localX, localZ);
                int min = minMax == null ? Integer.MAX_VALUE : minMax.getLeft();
                int max = minMax == null ? Integer.MIN_VALUE : minMax.getRight();
                minBlockYArr[localX][localZ] = min;
                maxBlockYArr[localX][localZ] = max;
                minMinHeight = Math.min(min, minMinHeight);
                maxMaxHeight = Math.max(max, maxMaxHeight);
            }
        }

        // Iterate over all affected cubes.
        Iterable<? extends ICube> cubes = column.getLoadedCubes(blockToCube(maxMaxHeight), blockToCube(/*minMinHeight*/Integer.MIN_VALUE));
        for (ICube otherCube : cubes) {
            int minCubeBlockY = otherCube.getCoords().getMinBlockY();
            int maxCubeBlockY = otherCube.getCoords().getMaxBlockY();
            for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
                    int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];

                    // If no update is needed, skip the block column.
                    if (minBlockY > maxBlockY) {
                        continue;
                    }
                    // if not in this cube, skip
                    if (!MathUtil.rangesIntersect(minBlockY, maxBlockY, minCubeBlockY, maxCubeBlockY)) {
                        continue;
                    }

                    if (otherCube != cube && !otherCube.isInitialLightingDone()) {
                        continue;
                    }

                    this.mutablePos.setPos(blockX, this.mutablePos.getY(), blockZ);
                    // Update the block column in this cube.
                    if (!diffuseSkylightInBlockColumn(lm, otherCube, this.mutablePos, minBlockY, maxBlockY)) {
                        throw new IllegalStateException("Check light failed at " + this.mutablePos + "!");
                    }
                }
            }
        }
    }

    /**
     * Diffuses skylight inside of the given cube in the block column specified by the given MutableBlockPos. The
     * update is limited vertically by minBlockY and maxBlockY.
     *
     * @param cube the cube inside of which the skylight is to be diffused
     * @param pos the xz-position of the block column to be updated
     * @param minBlockY the lower bound of the section to be updated
     * @param maxBlockY the upper bound of the section to be updated
     *
     * @return true if the update was successful, false otherwise
     */
    private boolean diffuseSkylightInBlockColumn(ILightingManager lm, ICube cube, MutableBlockPos pos, int minBlockY, int maxBlockY) {
        int cubeMinBlockY = cubeToMinBlock(cube.getY());
        int cubeMaxBlockY = cubeToMaxBlock(cube.getY());

        int maxBlockYInCube = Math.min(cubeMaxBlockY, maxBlockY);
        int minBlockYInCube = Math.max(cubeMinBlockY, minBlockY);

        for (int blockY = maxBlockYInCube; blockY >= minBlockYInCube; --blockY) {
            pos.setY(blockY);
            if (needsSkylightUpdate(cube, pos)) {
                lm.checkLightFor(EnumSkyBlock.SKY, pos);
            }
        }

        return true;
    }


    /**
     * Determines if the block at the given position requires a skylight update.
     *
     * @param pos the block's global position
     *
     * @return true if the specified block needs a skylight update, false otherwise
     */
    private static boolean needsSkylightUpdate(ICube cube, MutableBlockPos pos) {
        // Opaque blocks don't need update. Nothing can emit skylight, and skylight can't get into them nor out of them.
        IBlockAccess world = cube.getWorld();
        return cube.getBlockState(pos).getLightOpacity(world, pos) < 15;
    }

    /**
     * Determines which vertical section of the specified block column in the given cube requires a lighting update
     * based on the current occlusion in the cube's column.
     *
     * @param cube the cube inside of which the skylight is to be updated
     * @param localX the local x-coordinate of the block column
     * @param localZ the local z-coordinate of the block column
     *
     * @return a pair containing the minimum and the maximum y-coordinate to be updated in the given cube
     */
    @Nullable
    private static ImmutablePair<Integer, Integer> getMinMaxLightUpdateY(ICube cube, int localX, int localZ) {

        IColumn column = cube.getColumn();
        int heightMax = ((IColumnInternal) column).getHeightWithStaging(localX, localZ) - 1;//==Y of the top block

        // If the given cube is above the highest occluding block in the column, everything is fully lit.
        int cubeY = cube.getY();
        if (blockToCube(heightMax) < cubeY) {
            return null;
        }
        // If the given cube lies underneath the occluding block,
        // then only blocks in this cube need updating, already handled
        //if (cubeY < blockToCube(heightMax)) {
        //    return null;
        //}

        // ... otherwise, the update must start at the occluding block.
        int previousMaxHeight = column.getOpacityIndex().getTopBlockY(localX, localZ);
        //noinspection SuspiciousNameCombination
        return new ImmutablePair<>(previousMaxHeight, heightMax);
    }
}
