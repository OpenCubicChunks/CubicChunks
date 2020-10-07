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
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMaxBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.localToBlock;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.util.FastCubeBlockAccess;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: extract interfaces when it's done
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager implements ILightingManager {

    public static final boolean NO_SUNLIGHT_PROPAGATION = "true".equalsIgnoreCase(System.getProperty("cubicchunks.nosunlight"));

    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
    @Nonnull private World world;
    @Nonnull private LightPropagator lightPropagator = new LightPropagator();
    @Nonnull private final List<IHeightChangeListener> heightUpdateListeners = new ArrayList<>();
    @Nullable private LightUpdateTracker tracker;

    public LightingManager(World world) {
        this.world = world;

    }

    @Nullable
    private LightUpdateTracker getTracker() {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        if (tracker == null) {
            if (!world.isRemote) {
                tracker = new LightUpdateTracker((PlayerCubeMap) ((WorldServer) world).getPlayerChunkMap());
            }
        }
        return tracker;
    }
    /**
     * Registers height change listener, that receives all height changes after initial lighting is done
     *
     * @param listener height change listener
     */
    public void registerHeightChangeListener(IHeightChangeListener listener) {
        heightUpdateListeners.add(listener);
    }

    @Nullable
    public CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        if (!cube.getWorld().provider.hasSkyLight()) {
            return null;
        }
        return new CubeLightUpdateInfo(cube);
    }

    private void columnSkylightUpdate(UpdateType type, Chunk column, int localX, int minY, int maxY, int localZ) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        if (!world.provider.hasSkyLight()) {
            return;
        }
        int blockX = Coords.localToBlock(column.x, localX);
        int blockZ = Coords.localToBlock(column.z, localZ);

        if (type == UpdateType.IMMEDIATE) {
            TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
            TIntIterator it = toDiffuse.iterator();
            while (it.hasNext()) {
                int cubeY = it.next();
                ICube cube = ((IColumn) column).getCube(cubeY);
                boolean success = updateDiffuseLight(cube, localX, localZ, minY, maxY);
                if (!success) {
                    markCubeBlockColumnForUpdate(cube, blockX, blockZ);
                }
            }
        } else {
            assert type == UpdateType.QUEUED;
            TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
            TIntIterator it = toDiffuse.iterator();
            while (it.hasNext()) {
                int cubeY = it.next();
                markCubeBlockColumnForUpdate(((IColumn) column).getCube(cubeY), blockX, blockZ);
            }
        }
    }

    private boolean updateDiffuseLight(ICube cube, int localX, int localZ, int minY, int maxY) {
        int minCubeY = cube.getCoords().getMinBlockY();
        int maxCubeY = cube.getCoords().getMaxBlockY();

        int minInCubeY = MathHelper.clamp(minY, minCubeY, maxCubeY);
        int maxInCubeY = MathHelper.clamp(maxY, minCubeY, maxCubeY);

        if (minInCubeY > maxInCubeY) {
            return true;
        }
        int blockX = localToBlock(cube.getX(), localX);
        int blockZ = localToBlock(cube.getZ(), localZ);

        return this.relightMultiBlock(
                new BlockPos(blockX, minInCubeY, blockZ), new BlockPos(blockX, maxInCubeY, blockZ), EnumSkyBlock.SKY, world::notifyLightSet);
    }

    @Override public void doOnBlockSetLightUpdates(Chunk column, int localX, int y1, int y2, int localZ) {
        this.columnSkylightUpdate(UpdateType.IMMEDIATE, column, localX, Math.min(y1, y2), Math.max(y1, y2), localZ);
    }

    //TODO: make it private
    @Override public void markCubeBlockColumnForUpdate(ICube cube, int blockX, int blockZ) {
        CubeLightUpdateInfo data = ((Cube) cube).getCubeLightUpdateInfo();
        if (data != null) {
            data.markBlockColumnForUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
        }
    }

    @Override public void onHeightMapUpdate(Chunk column, int localX, int localZ, int oldHeight, int newHeight) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        int minCubeY = blockToCube(Math.min(oldHeight, newHeight));
        int maxCubeY = blockToCube(Math.max(oldHeight, newHeight));
        ((IColumn) column).getLoadedCubes().stream().filter(cube -> cube.getY() >= minCubeY && cube.getY() <= maxCubeY).forEach(cube -> {
            markCubeBlockColumnForUpdate(cube, localX, localZ);
        });
    }

    /**
     * Updates light for given block region.
     * <p>
     *
     * @param startPos the minimum block coordinates (inclusive)
     * @param endPos the maximum block coordinates (inclusive)
     * @param type the light type to update
     *
     * @return true if update was successful, false if it failed. If the method returns false, no light values are
     * changed.
     */
    boolean relightMultiBlock(BlockPos startPos, BlockPos endPos, EnumSkyBlock type, Consumer<BlockPos> notify) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return true;
        }
        // TODO: optimize if needed
        // TODO: Figure out why it crashes with value 17
        final int LOAD_RADIUS = 17;
        BlockPos midPos = Coords.midPos(startPos, endPos);
        BlockPos minLoad = startPos.add(-LOAD_RADIUS, -LOAD_RADIUS, -LOAD_RADIUS);
        BlockPos maxLoad = endPos.add(LOAD_RADIUS, LOAD_RADIUS, LOAD_RADIUS);
        ILightBlockAccess blocks = FastCubeBlockAccess.forBlockRegion((ICubeProviderInternal) world.getChunkProvider(), minLoad, maxLoad);
        this.lightPropagator.propagateLight(midPos, BlockPos.getAllInBox(startPos, endPos), blocks, type, notify);
        return true;
    }

    public void sendHeightMapUpdate(BlockPos pos) {
        int size = heightUpdateListeners.size();
        for (int i = 0; i < size; i++) {
            heightUpdateListeners.get(i).heightUpdated(pos.getX(), pos.getZ());
        }
    }

    private enum UpdateType {
        IMMEDIATE, QUEUED
    }

    //this will be interface
    public static class CubeLightUpdateInfo {

        private final Cube cube;
        private final boolean[] toUpdateColumns = new boolean[Cube.SIZE * Cube.SIZE];
        private boolean hasUpdates;
        /**
         * Do neighbor need a sky light update when it is loaded?
         */
        public EnumSet<EnumFacing> edgeNeedSkyLightUpdate = EnumSet.noneOf(EnumFacing.class);

        public CubeLightUpdateInfo(Cube cube) {
            this.cube = cube;
        }

        void markBlockColumnForUpdate(int localX, int localZ) {
            toUpdateColumns[index(localX, localZ)] = true;
            hasUpdates = true;
        }

        public void markEdgeNeedSkyLightUpdate(EnumFacing side) {
            edgeNeedSkyLightUpdate.add(side);
        }

        public void tick() {
            if (NO_SUNLIGHT_PROPAGATION) {
                return;
            }
            ICubicWorldInternal cubicWorld = cube.getWorld();
            LightingManager manager = cubicWorld.getLightingManager();
            LightUpdateTracker tracker = manager.getTracker();
            ICubeProviderInternal cache = cubicWorld.getCubeCache();

            for (EnumFacing dir : EnumFacing.values()) {
                if (edgeNeedSkyLightUpdate.contains(dir)) {
                    CubePos cpos = cube.getCoords();
                    Cube loadedCube = cache.getLoadedCube(
                            cpos.getX() + dir.getXOffset(),
                            cpos.getY() + dir.getYOffset(),
                            cpos.getZ() + dir.getZOffset());
                    if (loadedCube == null) {
                        continue;
                    }

                    int fromBlockX = cpos.getMinBlockX();
                    int fromBlockY = cpos.getMinBlockY();
                    int fromBlockZ = cpos.getMinBlockZ();
                    int toBlockX = cpos.getMaxBlockX();
                    int toBlockY = cpos.getMaxBlockY();
                    int toBlockZ = cpos.getMaxBlockZ();
                    switch (dir) {
                        case DOWN:
                            fromBlockY = fromBlockY - 1;
                            toBlockY = fromBlockY + 1;
                            break;
                        case UP:
                            toBlockY = toBlockY + 1;
                            fromBlockY = toBlockY - 1;
                            break;
                        case NORTH:
                            fromBlockZ = fromBlockZ - 1;
                            toBlockZ = fromBlockZ + 1;
                            break;
                        case SOUTH:
                            toBlockZ = toBlockZ + 1;
                            fromBlockZ = toBlockZ - 1;
                            break;
                        case WEST:
                            fromBlockX = fromBlockX - 1;
                            toBlockX = fromBlockX + 1;
                            break;
                        case EAST:
                            toBlockX = toBlockX + 1;
                            fromBlockX = toBlockX - 1;
                            break;
                    }
                    manager.relightMultiBlock(
                            new BlockPos(fromBlockX, fromBlockY, fromBlockZ),
                            new BlockPos(toBlockX, toBlockY, toBlockZ),
                            EnumSkyBlock.SKY, pos -> {
                                cube.getWorld().notifyLightSet(pos);
                                if (tracker != null) {
                                    tracker.onUpdate(pos);
                                }
                            });
                    edgeNeedSkyLightUpdate.remove(dir);
                    CubeLightUpdateInfo cubeLightUpdateInfo = loadedCube.getCubeLightUpdateInfo();
                    if (cubeLightUpdateInfo != null) {
                        cubeLightUpdateInfo.edgeNeedSkyLightUpdate.remove(dir);
                    }
                }
            }
            if (!this.hasUpdates) {
                return;
            }
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    if (!toUpdateColumns[index(localX, localZ)]) {
                        continue;
                    }
                    manager.relightMultiBlock(
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMinBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMaxBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            EnumSkyBlock.SKY, pos -> {
                                cube.getWorld().notifyLightSet(pos);
                                if (tracker != null) {
                                    tracker.onUpdate(pos);
                                }
                            }
                    );
                    toUpdateColumns[index(localX, localZ)] = false;
                }
            }
            this.hasUpdates = false;
        }

        private int index(int x, int z) {
            return x << 4 | z;
        }

        public boolean hasUpdates() {
            return hasUpdates || !edgeNeedSkyLightUpdate.isEmpty();
        }

        public void clear() {
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    toUpdateColumns[index(localX, localZ)] = false;
                }
            }
            hasUpdates = false;
        }
    }

    public interface IHeightChangeListener {

        void heightUpdated(int blockX, int blockZ);
    }
}
