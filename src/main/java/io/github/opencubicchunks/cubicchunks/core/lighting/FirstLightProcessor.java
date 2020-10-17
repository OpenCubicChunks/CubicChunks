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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMaxBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;

import io.github.opencubicchunks.cubicchunks.api.util.MathUtil;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.util.FastCubeBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.IntHash;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
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
//TODO: make it also update blocklight
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FirstLightProcessor {

    private static final IntHash.Strategy CUBE_Y_HASH = new IntHash.Strategy() {

        @Override
        public int hashCode(int e) {
            return e;
        }

        @Override
        public boolean equals(int a, int b) {
            return a == b;
        }
    };


    @Nonnull private final MutableBlockPos mutablePos = new MutableBlockPos();

    @Nonnull private final ICubeProviderInternal cache;

    @Nonnull private final LightPropagator propagator = new LightPropagator();
    private final LightUpdateTracker tracker;


    /**
     * Creates a new FirstLightProcessor for the given world.
     *
     * @param world the world for which the FirstLightProcessor will be used
     */
    public FirstLightProcessor(WorldServer world) {
        this.cache = (ICubeProviderInternal) world.getChunkProvider();
        LightingManager lightingManager = ((ICubicWorldInternal) world).getLightingManager();
        this.tracker = lightingManager.getTracker();
    }

    /**
     * Diffuses skylight in the given cube and all cubes affected by this update.
     *
     * @param cube the cube whose skylight is to be initialized
     */
    public void diffuseSkylight(Cube cube) {
        if (LightingManager.NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        FastCubeBlockAccess access = new FastCubeBlockAccess(this.cache, cube, 2);

        Iterable<? extends BlockPos> allBlocks = BlockPos.getAllInBoxMutable(
                cube.getCoords().getMinBlockPos().add(-1, -1, -1),
                cube.getCoords().getMaxBlockPos().add(1, 1, 1)
        );
        if (cube.isEmpty()) {
            List<BlockPos> positions = new ArrayList<>();
            for (BlockPos pos : allBlocks) {
                int localX = blockToLocal(pos.getX());
                int localY = blockToLocal(pos.getY());
                int localZ = blockToLocal(pos.getZ());
                // add edges of current and neighbor cube
                if (localX == 15 || localX == 0 || localY == 15 || localY == 0 || localZ == 15 || localZ == 0) {
                    positions.add(pos.toImmutable());
                }
            }
            propagator.propagateLight(cube.getCoords().getCenterBlockPos(),
                    positions, access, EnumSkyBlock.BLOCK, false, pos -> {});
        } else {
            propagator.propagateLight(cube.getCoords().getCenterBlockPos(),
                    allBlocks, access, EnumSkyBlock.BLOCK, false, pos -> {});
        }


        if (!cube.getWorld().provider.hasSkyLight()) {
            return;
        }
        propagator.propagateLight(cube.getCoords().getCenterBlockPos(),
                allBlocks, access, EnumSkyBlock.SKY, false, tracker::onUpdate);

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
        for (int localX = 0; localX <= Cube.SIZE - 1; ++localX) {
            for (int localZ = 0; localZ <= Cube.SIZE - 1; ++localZ) {
                Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cube, localX, localZ);
                int min = minMax == null ? Integer.MAX_VALUE : minMax.getLeft();
                int max = minMax == null ? Integer.MIN_VALUE : minMax.getRight();
                minBlockYArr[localX][localZ] = min;
                maxBlockYArr[localX][localZ] = max;
                minMinHeight = Math.min(min, minMinHeight);
                maxMaxHeight = Math.max(max, maxMaxHeight);
            }
        }

        Int2ObjectMap<FastCubeBlockAccess> blockAccessMap = new Int2ObjectOpenCustomHashMap<>(10, 0.75f, CUBE_Y_HASH);

        List<BlockPos> toUpdate = new ArrayList<>();

        IColumn column = cube.getColumn();
        // Iterate over all affected cubes.
        Iterable<? extends ICube> cubes = column.getLoadedCubes(blockToCube(maxMaxHeight), blockToCube(minMinHeight));
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
                    if (!diffuseSkylightInBlockColumn(otherCube, this.mutablePos, minBlockY, maxBlockY, blockAccessMap, toUpdate)) {
                        throw new IllegalStateException("Check light failed at " + this.mutablePos + "!");
                    }
                }
            }
            if (!toUpdate.isEmpty()) {
                propagator.propagateLight(otherCube.getCoords().getCenterBlockPos(), toUpdate,
                        blockAccessMap.get(otherCube.getY()), EnumSkyBlock.SKY, tracker::onUpdate);
                toUpdate.clear();
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
    private boolean diffuseSkylightInBlockColumn(ICube cube, MutableBlockPos pos, int minBlockY, int maxBlockY,
            Int2ObjectMap<FastCubeBlockAccess> blockAccessMap, List<BlockPos> posToUpdate) {
        int cubeMinBlockY = cubeToMinBlock(cube.getY());
        int cubeMaxBlockY = cubeToMaxBlock(cube.getY());

        int maxBlockYInCube = Math.min(cubeMaxBlockY, maxBlockY);
        int minBlockYInCube = Math.max(cubeMinBlockY, minBlockY);

        FastCubeBlockAccess blockAccess = blockAccessMap.get(cube.getY());
        if (blockAccess == null) {
            blockAccess = new FastCubeBlockAccess(this.cache, cube, 1);
            blockAccessMap.put(cube.getY(), blockAccess);
        }

        for (int blockY = maxBlockYInCube; blockY >= minBlockYInCube; --blockY) {
            pos.setY(blockY);
            if (needsSkylightUpdate(blockAccess, pos)) {
                posToUpdate.add(pos.toImmutable());
            }
        }

        return true;
    }


    /**
     * Determines if the block at the given position requires a skylight update.
     *
     * @param access a FastCubeBlockAccess providing access to the block
     * @param pos the block's global position
     *
     * @return true if the specified block needs a skylight update, false otherwise
     */
    private static boolean needsSkylightUpdate(@Nonnull ILightBlockAccess access, @Nonnull MutableBlockPos pos) {

        // Opaque blocks don't need update. Nothing can emit skylight, and skylight can't get into them nor out of them.
        if (access.getBlockLightOpacity(pos) >= 15) {
            return false;
        }

        // This is the logic that world.checkLightFor uses to determine if it should continue updating.
        // This is done here to avoid isAreaLoaded call (a lot of them quickly add up to a lot of time).
        // It first calculates the expected skylight value of this block and then it checks the neighbors' saved values,
        // if the saved value matches the expected value, it will be updated.
        int computedLight = access.computeLightValue(pos);
        if (computedLight != access.getLightFor(EnumSkyBlock.SKY, pos)) {
            return true;
        }
        for (EnumFacing facing : EnumFacing.values()) {
            pos.move(facing);
            int currentLight = access.getLightFor(EnumSkyBlock.SKY, pos);
            int currentOpacity = Math.max(1, access.getBlockLightOpacity(pos));
            pos.move(facing.getOpposite());

            if (computedLight == currentLight - currentOpacity) {
                return true;
            }
        }
        return false;
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
    private static ImmutablePair<Integer, Integer> getMinMaxLightUpdateY(@Nonnull Cube cube, int localX, int localZ) {

        IColumn column = cube.getColumn();
        int heightMax = ((IColumnInternal) column).getHeightWithStaging(localX, localZ) - 1;//==Y of the top block

        // If the given cube is above the highest occluding block in the column, everything is fully lit.
        int cubeY = cube.getY();
        if (blockToCube(heightMax) < cubeY) {
            return null;
        }
        // If the given cube lies underneath the occluding block,
        // then only blocks in this cube need updating, already handled
        if (cubeY < blockToCube(heightMax)) {
            return null;
        }

        // ... otherwise, the update must start at the occluding block.
        int previousMaxHeight = column.getOpacityIndex().getTopBlockY(localX, localZ);
        //noinspection SuspiciousNameCombination
        return new ImmutablePair<>(previousMaxHeight, heightMax);
    }
}
