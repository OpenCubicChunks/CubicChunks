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
package cubicchunks.lighting;

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.util.Coords.cubeToMaxBlock;
import static cubicchunks.util.Coords.cubeToMinBlock;
import static cubicchunks.util.Coords.getCubeCenter;

import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.util.MathUtil;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.IntHash;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private static final int LIGHT_UPDATE_RADIUS = 17;

    private static final int CUBE_RADIUS = Cube.SIZE / 2;

    private static final int UPDATE_BUFFER_RADIUS = 1;

    private static final int UPDATE_RADIUS = LIGHT_UPDATE_RADIUS + CUBE_RADIUS + UPDATE_BUFFER_RADIUS;

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

    @Nonnull private final ICubeProvider cache;

    @Nonnull private final LightPropagator propagator = new LightPropagator();
    @Nonnull private final LightUpdateTracker tracker;


    /**
     * Creates a new FirstLightProcessor for the given world.
     *
     * @param world the world for which the FirstLightProcessor will be used
     */
    public FirstLightProcessor(ICubicWorldServer world) {
        this.cache = world.getCubeCache();
        this.tracker = new LightUpdateTracker(world.getPlayerCubeMap());
    }


    /**
     * Initializes skylight in the given cube. The skylight will be consistent with respect to the world configuration
     * and already existing cubes. It is however possible for cubes being considered lit at this stage to be occluded
     * by cubes being generated further up.
     *
     * @param cube the cube whose skylight is to be initialized
     */
    public void initializeSkylight(Cube cube) {
        if (!cube.getCubicWorld().getProvider().hasSkyLight()) {
            return;
        }

        IHeightMap opacityIndex = cube.getColumn().getOpacityIndex();

        int cubeMinY = cubeToMinBlock(cube.getY());

        BlockPos startPos = cube.getCoords().getMinBlockPos();

        for (int localX = 0; localX < Cube.SIZE; ++localX) {
            for (int localZ = 0; localZ < Cube.SIZE; ++localZ) {
                for (int localY = Cube.SIZE - 1; localY >= 0; --localY) {

                    if (opacityIndex.isOccluded(localX, cubeMinY + localY, localZ)) {
                        break;
                    }

                    cube.setLightFor(EnumSkyBlock.SKY, startPos.add(localX, localY, localZ), 15);
                }
            }
        }
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
        if (!cube.getCubicWorld().getProvider().hasSkyLight()) {
            cube.setInitialLightingDone(true);
            return;
        }
        ICubicWorld world = cube.getCubicWorld();

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

        IColumn IColumn = cube.getColumn();
        // Iterate over all affected cubes.
        Iterable<Cube> cubes = IColumn.getLoadedCubes(blockToCube(maxMaxHeight), blockToCube(minMinHeight));
        for (Cube otherCube : cubes) {
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

                    if (otherCube != cube && !cube.isInitialLightingDone()) {
                        continue;
                    }

                    this.mutablePos.setPos(blockX, this.mutablePos.getY(), blockZ);
                    int topBlockY = getOcclusionHeight(IColumn, blockToLocal(blockX), blockToLocal(blockZ));

                    if (otherCube != cube && canStopUpdating(cube, this.mutablePos, topBlockY)) {
                        // mark this column so min > max
                        minBlockYArr[blockX - minBlockX][blockZ - minBlockZ] = 1;
                        maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ] = 0;
                        continue;
                    }

                    // Skip this cube if an update is not possible.
                    if (!canUpdateCube(otherCube)) {
                        // Queue the update to be processed once the cube is ready for it.
                        world.getLightingManager().markCubeBlockColumnForUpdate(otherCube, this.mutablePos.getX(), this.mutablePos.getZ());
                        continue;
                    }

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
        tracker.sendAll();
        cube.setInitialLightingDone(true);
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
    private boolean diffuseSkylightInBlockColumn(Cube cube, MutableBlockPos pos, int minBlockY, int maxBlockY,
            Int2ObjectMap<FastCubeBlockAccess> blockAccessMap, List<BlockPos> posToUpdate) {
        int cubeMinBlockY = cubeToMinBlock(cube.getY());
        int cubeMaxBlockY = cubeToMaxBlock(cube.getY());

        int maxBlockYInCube = Math.min(cubeMaxBlockY, maxBlockY);
        int minBlockYInCube = Math.max(cubeMinBlockY, minBlockY);

        FastCubeBlockAccess blockAccess = blockAccessMap.get(cube.getY());
        if (blockAccess == null) {
            // this value will be reused later for LightPropagator, so use radius 2
            blockAccess = new FastCubeBlockAccess(this.cache, cube, 2);
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
    private static boolean needsSkylightUpdate(@Nonnull FastCubeBlockAccess access, @Nonnull MutableBlockPos pos) {

        // Opaque blocks don't need update. Nothing can emit skylight, and skylight can't get into them nor out of them.
        if (access.getBlockLightOpacity(pos) >= 15) {
            return false;
        }

        // This is the logic that world.checkLightFor uses to determine if it should continue updating.
        // This is done here to avoid isAreaLoaded call (a lot of them quickly add up to a lot of time).
        // It first calculates the expected skylight value of this block and then it checks the neighbors' saved values,
        // if the saved value matches the expected value, it will be updated.
        int computedLight = access.computeLightValue(pos);
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
     * Determines if light in the given cube can be updated.
     *
     * @param cube the cube whose light is supposed to be updated
     *
     * @return true if light in the given cube can be updated, false otherwise
     */
    private static boolean canUpdateCube(@Nonnull Cube cube) {
        BlockPos cubeCenter = getCubeCenter(cube);
        return cube.getCubicWorld().testForCubes(cubeCenter, UPDATE_RADIUS, Objects::nonNull);
    }

    /**
     * Determines if the block column of the given cube as specified by the given BlockPos has valid lighting and thus
     * does not require further updating.
     *
     * @param cube the cube whose light is supposed to be updated
     * @param pos the xz-position of the block column being updated
     * @param topBlockY the y-coordinate of the highest block in the block column
     *
     * @return true if updating the skylight of the specified block column is no longer required, false otherwise
     */
    private static boolean canStopUpdating(@Nonnull Cube cube, @Nonnull MutableBlockPos pos, int topBlockY) {
        // Note: This logic does not apply to the main cube being updated, but only to those below it!
        pos.setY(cube.getCoords().getMaxBlockY());
        boolean isDirectSkylight = pos.getY() > topBlockY;
        int lightValue = cube.getLightFor(EnumSkyBlock.SKY, pos);

        // If the cube does not receive direct skylight and the light value does not need updating, then all blocks
        // further down do not need to be updated either.
        return !isDirectSkylight && lightValue < 15;
    }

    /**
     * Returns the y-coordinate of the highest occluding block in the specified block column. If there exists no such
     * block {@link cubicchunks.util.Coords#NO_HEIGHT} will be returned instead.
     *
     * @param IColumn the column containing the block column
     * @param localX the block column's local x-coordinate
     * @param localZ the block column's local z-coordinate
     *
     * @return the y-coordinate of the highest occluding block in the specified block column or {@link
     * cubicchunks.util.Coords#NO_HEIGHT} if no such block exists
     */
    private static int getOcclusionHeight(@Nonnull IColumn IColumn, int localX, int localZ) {
        return IColumn.getOpacityIndex().getTopBlockY(localX, localZ);
    }

    /**
     * Returns the y-coordinate of the highest occluding block in the specified block column, that is underneath the
     * cube at the given y-coordinate. If there exists no such block {@link cubicchunks.util.Coords#NO_HEIGHT} will be
     * returned instead.
     *
     * @param IColumn the column containing the block column
     * @param blockX the block column's global x-coordinate
     * @param blockZ the block column's global z-coordinate
     * @param cubeY the y-coordinate of the cube underneath which the highest occluding block is to be found
     *
     * @return the y-coordinate of the highest occluding block underneath the given cube in the specified block column
     * or {@link cubicchunks.util.Coords#NO_HEIGHT} if no such block exists
     */
    private static int getOcclusionHeightBelowCubeY(@Nonnull IColumn IColumn, int blockX, int blockZ, int cubeY) {
        IHeightMap index = IColumn.getOpacityIndex();
        return index.getTopBlockYBelow(blockToLocal(blockX), blockToLocal(blockZ), cubeToMinBlock(cubeY));
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

        IColumn IColumn = cube.getColumn();
        int heightMax = getOcclusionHeight(IColumn, localX, localZ);//==Y of the top block

        // If the given cube is above the highest occluding block in the column, everything is fully lit.
        int cubeY = cube.getY();
        if (blockToCube(heightMax) < cubeY) {
            return null;
        }

        int blockX = cubeToMinBlock(cube.getX()) + localX;
        int blockZ = cubeToMinBlock(cube.getZ()) + localZ;

        // If the given cube lies underneath the occluding block, it must be updated from the top down.
        if (cubeY < blockToCube(heightMax)) {

            // Determine the y-coordinate of the highest block (and its cube) occluding blocks inside of the given cube
            // or further down.
            int topBlockYInThisCubeOrBelow = getOcclusionHeightBelowCubeY(IColumn, blockX, blockZ, cube.getY() + 1);
            int topBlockCubeYInThisCubeOrBelow = blockToCube(topBlockYInThisCubeOrBelow);

            // If the given cube contains the occluding block, the update can be limited down to that block.
            if (topBlockCubeYInThisCubeOrBelow == cubeY) {
                int heightBelowCube = getOcclusionHeightBelowCubeY(IColumn, blockX, blockZ, cube.getY()) + 1;
                //noinspection SuspiciousNameCombination
                return new ImmutablePair<>(heightBelowCube, cubeToMaxBlock(cubeY));
            }
            // Otherwise, the whole height of the cube must be updated.
            else {
                return new ImmutablePair<>(cubeToMinBlock(cubeY), cubeToMaxBlock(cubeY));
            }
        }

        // ... otherwise, the update must start at the occluding block.
        int heightBelowCube = getOcclusionHeightBelowCubeY(IColumn, blockX, blockZ, cubeY);
        //noinspection SuspiciousNameCombination
        return new ImmutablePair<>(heightBelowCube, heightMax);
    }
}
