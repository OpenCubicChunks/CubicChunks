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
import static cubicchunks.util.Coords.localToBlock;

import cubicchunks.util.CubePos;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.IntHash;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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

    private ILightChunkAccess chunkAccess;
    private final LightPropagator propagator;
    private final LightUpdateTracker tracker;
    private EnumSet<EnumSkyBlock> typesToUpdate;


    /**
     * Creates a new FirstLightProcessor for the given world.
     *
     * @param chunkAccess instance of @{@link ILightChunkAccess}
     * @param updateTracker all changed light values go through this, used to send light updates to client
     * @param propagator spreads out light
     */
    public FirstLightProcessor(ILightChunkAccess chunkAccess, LightUpdateTracker updateTracker,
            LightPropagator propagator, EnumSet<EnumSkyBlock> typesToUpdate) {
        this.chunkAccess = chunkAccess;
        this.propagator = propagator;
        this.tracker = updateTracker;
        this.typesToUpdate = typesToUpdate;
    }

    /**
     * Updates skylight in the given cube and all cubes affected by this update.
     *
     * @param cubePos the cube pos whose lighting is to be initialized
     */
    public void updateSkylightFor(CubePos cubePos) {
        if (typesToUpdate.isEmpty()) {
            return;
        }
        ILightBlockAccess cubeBlocks = chunkAccess.getLightBlockAccess(cubePos);

        List<BlockPos> blockUpdates = new ArrayList<>();
        List<BlockPos> skyUpdates = new ArrayList<>();

        for (int localX = 0; localX < Cube.SIZE; localX++) {
            for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                if (typesToUpdate.contains(EnumSkyBlock.BLOCK)) {
                    updateBlockLight(blockUpdates, cubeBlocks, cubePos, localX, localZ);
                }
                if (typesToUpdate.contains(EnumSkyBlock.SKY)) {
                    updateSkyLight(skyUpdates, cubeBlocks, cubePos, localX, localZ);
                }
            }
        }
        BlockPos center = cubePos.getCenterBlockPos();
        if (!blockUpdates.isEmpty()) {
            propagator.propagateLight(center, blockUpdates, cubeBlocks, EnumSkyBlock.BLOCK, LightPropagator.NULL_CALLBACK);
        }

        if (!skyUpdates.isEmpty()) {
            propagator.propagateLight(center, skyUpdates, cubeBlocks, EnumSkyBlock.SKY, LightPropagator.NULL_CALLBACK);
        }
    }

    private void updateBlockLight(List<BlockPos> toUpdate, ILightBlockAccess blocks, CubePos cubePos, int localX, int localZ) {
        for (int localY = 0; localY < Cube.SIZE; localY++) {
            BlockPos pos = cubePos.localToBlock(localX, localY, localZ);
            if (blocks.getEmittedLight(pos, EnumSkyBlock.BLOCK) > 0) {
                toUpdate.add(pos);
            }
        }
    }

    private void updateSkyLight(List<BlockPos> toUpdate, ILightBlockAccess cubeBlocks, CubePos cubePos, int localX, int localZ) {
        int topY = cubeBlocks.getEffectiveTopBlockY(cubePos, cubePos.localToBlock(localX, 0, localZ));
        int maxCubeBlockY = cubePos.getMaxBlockY();
        for (int blockY = maxCubeBlockY; blockY > topY; blockY--) {
            // no opacity checks, blocks in-between can't be opaque because they are all above topY
            toUpdate.add(new BlockPos(localToBlock(cubePos.getX(), localX), blockY, localToBlock(cubePos.getZ(), localZ)));
        }
        if (topY > maxCubeBlockY) {
            // edge case:
        }
        // the edges are a case: when spreading light into neighbor uncommitted cube above the top blocks, light won't spread in because
        // all blocks have direct sunlight. This code updates the edge lights in that cube after the fact (only if the neighbor cube already has
        // first light done, in places that right now became
    }
}
