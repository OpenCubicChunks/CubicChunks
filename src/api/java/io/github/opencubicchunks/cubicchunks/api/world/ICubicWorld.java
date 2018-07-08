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
package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
     */
    ICubeProvider getCubeCache();

    /**
     * Finds the top block for that population cube with give offset, or null if no suitable place found.
     * This method starts from the top of population area (or forcedAdditionalCubes*16 blocks above that)
     * and goes down scanning for solid block. The value is used only if it's within population area.
     *
     * Note: forcedAdditionalCubes should be zero unless absolutely necessary.
     * TODO: make it go up instead of down so it doesn't load unnecessary chunks when forcedAdditionalCubes is nonzero
     */
    @Nullable
    public default BlockPos getSurfaceForCube(CubePos pos, int xOffset, int zOffset, int forcedAdditionalCubes, SurfaceType type) {
        int maxFreeY = pos.getMaxBlockY() + ICube.SIZE / 2;
        int minFreeY = pos.getMinBlockY() + ICube.SIZE / 2;
        int startY = pos.above().getMaxBlockY() + forcedAdditionalCubes * ICube.SIZE;

        BlockPos start = new BlockPos(
                pos.getMinBlockX() + xOffset,
                startY,
                pos.getMinBlockZ() + zOffset
        );
        return findTopBlock(start, minFreeY, maxFreeY, type);
    }

    @Nullable
    public default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, SurfaceType type) {
        BlockPos pos = start;
        IBlockState startState = ((World) this).getBlockState(start);
        if (canBeTopBlock(pos, startState, type)) {
            // the top tested block is solid, don't use that one
            return null;
        }
        while (pos.getY() >= minTopY) {
            BlockPos next = pos.down();
            IBlockState state = ((World) this).getBlockState(next);
            if (canBeTopBlock(pos, state, type)) {
                break;
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

    ICube getCubeFromBlockCoords(BlockPos pos);

    int getEffectiveHeight(int blockX, int blockZ);

    boolean isBlockColumnLoaded(BlockPos pos);

    boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty);

    int getMinGenerationHeight();

    int getMaxGenerationHeight();

    public enum SurfaceType {
        SOLID, BLOCKING_MOVEMENT, OPAQUE
    }
}
