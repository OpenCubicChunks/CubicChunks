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
package io.github.opencubicchunks.cubicchunks.api.world;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ICubicWorld extends IMinMaxHeight {

    boolean isCubicWorld();

    /**
     * Returns the {@link ICubeProvider} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     *
     * @return the cube provider
     */
    ICubeProvider getCubeCache();

    /**
     * Finds the top block for that population cube with given offset, or null if no suitable place found.
     * This method starts from the top of population area (or forcedAdditionalCubes*16 blocks above that)
     * and goes down scanning for solid block. The value is used only if it's within population area.
     *
     * Note: forcedAdditionalCubes should be zero unless absolutely necessary.
     *
     * @param cubePos cube position to find surface for
     * @param xOffset x coordinate of population area offset relative to cube origin
     * @param zOffset z coordinate of population area offset relative to cube origin
     * @param forcedAdditionalCubes amount of additional cubes above to scan
     * @param type surface type
     * @return position of the block above the top block matching criteria specified by surface type, or null if it doesn't exist
     */
    public default BlockPos getSurfaceForCube(CubePos cubePos, int xOffset, int zOffset, int forcedAdditionalCubes, SurfaceType type) {
        return getSurfaceForCube(cubePos, xOffset, zOffset, forcedAdditionalCubes, (pos, state) -> canBeTopBlock(pos, state, type));
    }

    @Nullable
    public default BlockPos getSurfaceForCube(CubePos pos, int xOffset, int zOffset, int forcedAdditionalCubes, BiPredicate<BlockPos, IBlockState> canBeTopBlock) {
        int maxFreeY = pos.getMaxBlockY() + ICube.SIZE / 2;
        int minFreeY = pos.getMinBlockY() + ICube.SIZE / 2;
        int startY = pos.above().getMaxBlockY() + forcedAdditionalCubes * ICube.SIZE;

        BlockPos start = new BlockPos(
                pos.getMinBlockX() + xOffset,
                startY,
                pos.getMinBlockZ() + zOffset
        );
        return findTopBlock(start, minFreeY, maxFreeY, canBeTopBlock);
    }

    @Nullable
    default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, SurfaceType type) {
        return findTopBlock(start, minTopY, maxTopY, (pos, state) -> canBeTopBlock(pos, state, type));
    }

    /**
     * Finds the top block between minTopY and maxTopY, startiung the search at start.
     * If a potential top block is found above maxTopY, this method returns null.
     *
     * Note that canBeTopBlock should return true even if you don't intend to generate something when that block is encountered
     * as long as it counts as "surface".
     *
     * @param start start position
     * @param minTopY minimum Y coordinate to search
     * @param maxTopY maximum Y coordinate to consider as a surface
     * @param canBeTopBlock checks whether a block at given position should be considered "surface".
     * @return the top found position
     */
    @Nullable
    default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, BiPredicate<BlockPos, IBlockState> canBeTopBlock) {
        BlockPos pos = start;
        IBlockState startState = ((World) this).getBlockState(start);
        if (canBeTopBlock.test(start, startState)) {
            // the top tested block is "top", don't use that one because we don't know what is above
            return null;
        }
        ICube cube = getCubeFromBlockCoords(pos.down());
        while (pos.getY() >= minTopY) {
            BlockPos next = pos.down();
            if (blockToCube(next.getY()) != cube.getY()) {
                cube = getCubeFromBlockCoords(next);
            }
            if (!cube.isEmpty()) {
                IBlockState state = cube.getBlockState(next);
                if (canBeTopBlock.test(next, state)) {
                    break;
                }
            }
            pos = next;
        }
        if (pos.getY() < minTopY || pos.getY() > maxTopY) {
            return null;
        }
        return pos;
    }

    public default boolean canBeTopBlock(BlockPos pos, IBlockState state, SurfaceType type) {
        switch (type) {
            case SOLID: {
                return state.getMaterial().blocksMovement()
                        && !state.getBlock().isLeaves(state, (World) this, pos)
                        && !state.getBlock().isFoliage((World) this, pos);
            }
            case OPAQUE: {
                return state.getLightOpacity((World) this, pos) != 0;
            }
            case BLOCKING_MOVEMENT: {
                return state.getMaterial().blocksMovement() || state.getMaterial().isLiquid();
            }
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions within blockRadius from
     * centerPos. Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     *
     * @param centerPos position to start at
     * @param blockRadius radius in block to test, starting from centerPos
     * @param test the test to apply
     * @return false if any invokation of the given predicate returns false, true otherwise
     */
    default boolean testForCubes(BlockPos centerPos, int blockRadius, Predicate<ICube> test) {
        return testForCubes(
                centerPos.getX() - blockRadius, centerPos.getY() - blockRadius, centerPos.getZ() - blockRadius,
                centerPos.getX() + blockRadius, centerPos.getY() + blockRadius, centerPos.getZ() + blockRadius,
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions between
     * BlockPos(minBlockX, minBlockY, minBlockZ) and BlockPos(maxBlockX, maxBlockY, maxBlockZ) (including the specified
     * positions). Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     *
     * @param minBlockX minimum block x coordinate
     * @param minBlockY minimum block y coordinate
     * @param minBlockZ minimum block z coordinate
     * @param maxBlockX maximum block x coordinate
     * @param maxBlockY maximum block y coordinate
     * @param maxBlockZ maximum block z coordinate
     * @param test the test to apply
     * @return false if any invokation of the given predicate returns false, true otherwise
     */
    default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, Predicate<ICube> test) {
        return testForCubes(
                CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ),
                CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ),
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for given cube and neighbors.
     * Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     *
     * @param start start cube position
     * @param end end cube position
     * @param test the test to apply
     * @return false if any invokation of the given predicate returns false, true otherwise

     */
    boolean testForCubes(CubePos start, CubePos end, Predicate<? super ICube> test);

    /**
     * Return the actual world height for this world. Typically this is 256 for worlds with a sky, and 128 for worlds
     * without.
     *
     * @return The actual world height
     */
    int getActualHeight();

    ICube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    default ICube getCubeFromCubeCoords(CubePos pos) {
        return getCubeFromCubeCoords(pos.getX(), pos.getY(), pos.getZ());
    }


    ICube getCubeFromBlockCoords(BlockPos pos);

    int getEffectiveHeight(int blockX, int blockZ);

    boolean isBlockColumnLoaded(BlockPos pos);

    boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty);

    int getMinGenerationHeight();

    int getMaxGenerationHeight();

    enum SurfaceType {
        SOLID, BLOCKING_MOVEMENT, OPAQUE
    }
}
