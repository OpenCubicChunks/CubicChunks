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
import static cubicchunks.util.Coords.cubeToMaxBlock;
import static cubicchunks.util.Coords.cubeToMinBlock;
import static cubicchunks.util.Coords.localToBlock;

import cubicchunks.server.PlayerCubeMap;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: extract interfaces when it's done
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager {

    public static final boolean NO_SUNLIGHT_PROPAGATION = "true".equalsIgnoreCase(System.getProperty("cubicchunks.nosunlight"));

    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
    @Nonnull private ICubicWorld world;
    @Nonnull private LightPropagator lightPropagator = new LightPropagator();
    @Nonnull private final List<IHeightChangeListener> heightUpdateListeners = new ArrayList<>();
    @Nullable private LightUpdateTracker tracker;

    public LightingManager(ICubicWorld world) {
        this.world = world;

    }

    @Nullable
    private LightUpdateTracker getTracker() {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        if (tracker == null) {
            if (!world.isRemote()) {
                tracker = new LightUpdateTracker((PlayerCubeMap) ((WorldServer) world).getPlayerChunkMap());
            }
        }
        return tracker;
    }
    /**
     * Registers height change listener, that receives all height changes after initial lighting is done
     */
    public void registerHeightChangeListener(IHeightChangeListener listener) {
        heightUpdateListeners.add(listener);
    }

    @Nullable
    public CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        if (!cube.getCubicWorld().getProvider().hasSkyLight()) {
            return null;
        }
        return new CubeLightUpdateInfo(cube);
    }

    private void columnSkylightUpdate(UpdateType type, IColumn column, int localX, int minY, int maxY, int localZ) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        if (!world.getProvider().hasSkyLight()) {
            return;
        }
        int blockX = Coords.localToBlock(column.getX(), localX);
        int blockZ = Coords.localToBlock(column.getZ(), localZ);

        if (type == UpdateType.IMMEDIATE) {
            TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
            TIntIterator it = toDiffuse.iterator();
            while (it.hasNext()) {
                int cubeY = it.next();
                boolean success = updateDiffuseLight(column.getCube(cubeY), localX, localZ, minY, maxY);
                if (!success) {
                    markCubeBlockColumnForUpdate(column.getCube(cubeY), blockX, blockZ);
                }
            }
        } else {
            assert type == UpdateType.QUEUED;
            TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
            TIntIterator it = toDiffuse.iterator();
            while (it.hasNext()) {
                int cubeY = it.next();
                markCubeBlockColumnForUpdate(column.getCube(cubeY), blockX, blockZ);
            }
        }
    }

    private boolean updateDiffuseLight(Cube cube, int localX, int localZ, int minY, int maxY) {
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

    public void doOnBlockSetLightUpdates(IColumn column, int localX, int oldHeight, int changeY, int localZ) {
        this.columnSkylightUpdate(UpdateType.IMMEDIATE, column, localX, Math.min(oldHeight, changeY), Math.max(oldHeight, changeY), localZ);
    }

    //TODO: make it private
    public void markCubeBlockColumnForUpdate(Cube cube, int blockX, int blockZ) {
        CubeLightUpdateInfo data = cube.getCubeLightUpdateInfo();
        if (data != null) {
            data.markBlockColumnForUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
        }
    }

    public void onHeightMapUpdate(IColumn IColumn, int localX, int localZ, int oldHeight, int newHeight) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        int minCubeY = blockToCube(Math.min(oldHeight, newHeight));
        int maxCubeY = blockToCube(Math.max(oldHeight, newHeight));
        IColumn.getLoadedCubes().stream().filter(cube -> cube.getY() >= minCubeY && cube.getY() <= maxCubeY).forEach(cube -> {
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
        ILightBlockAccess blocks = FastCubeBlockAccess.forBlockRegion(world.getCubeCache(), minLoad, maxLoad);
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

        public CubeLightUpdateInfo(Cube cube) {
            this.cube = cube;
        }

        void markBlockColumnForUpdate(int localX, int localZ) {
            toUpdateColumns[index(localX, localZ)] = true;
            hasUpdates = true;
        }

        public void tick() {
            if (NO_SUNLIGHT_PROPAGATION) {
                return;
            }
            LightUpdateTracker tracker = cube.getCubicWorld().getLightingManager().getTracker();
            for (EnumFacing dir : EnumFacing.values()) {
                if (cube.edgeNeedSkyLightUpdate[dir.ordinal()]) {
                    ICubeProvider cache = cube.getCubicWorld().getCubeCache();
                    CubePos cpos = cube.getCoords();
                    Cube loadedCube = cache.getLoadedCube(
                            cpos.getX() + dir.getFrontOffsetX(),
                            cpos.getY() + dir.getFrontOffsetY(),
                            cpos.getZ() + dir.getFrontOffsetZ());
                    if (loadedCube == null)
                        continue;
                    LightingManager manager = cube.getCubicWorld().getLightingManager();
                    int fromBlockX = cpos.getMinBlockX();
                    int fromBlockY = cpos.getMinBlockY();
                    int fromBlockZ = cpos.getMinBlockZ();
                    int toBlockX = cpos.getMaxBlockX();
                    int toBlockY = cpos.getMaxBlockY();
                    int toBlockZ = cpos.getMaxBlockZ();
                    boolean extendBack = loadedCube.edgeNeedSkyLightUpdate[dir.getOpposite().ordinal()];
                    switch (dir) {
                        case DOWN:
                            fromBlockY = fromBlockY - 1;
                            toBlockY = extendBack ? fromBlockY + 1 : fromBlockY;
                            break;
                        case UP:
                            toBlockY = toBlockY + 1;
                            fromBlockY = extendBack ? toBlockY - 1 : toBlockY;
                            break;
                        case NORTH:
                            fromBlockZ = fromBlockZ - 1;
                            toBlockZ = extendBack ? fromBlockZ + 1 : fromBlockZ;
                            break;
                        case SOUTH:
                            toBlockZ = toBlockZ + 1;
                            fromBlockZ = extendBack ? toBlockZ - 1 : toBlockZ;
                            break;
                        case WEST:
                            fromBlockX = fromBlockX - 1;
                            toBlockX = extendBack ? fromBlockX + 1 : fromBlockX;
                            break;
                        case EAST:
                            toBlockX = toBlockX + 1;
                            fromBlockX = extendBack ? toBlockX - 1 : toBlockX;
                            break;
                    }
                    manager.relightMultiBlock(
                            new BlockPos(fromBlockX, fromBlockY, fromBlockZ),
                            new BlockPos(toBlockX, toBlockY, toBlockZ),
                            EnumSkyBlock.SKY, pos -> {
                                cube.getCubicWorld().notifyLightSet(pos);
                                if (tracker != null) {
                                    tracker.onUpdate(pos);
                                }
                            });
                    cube.edgeNeedSkyLightUpdate[dir.ordinal()] = false;
                    loadedCube.edgeNeedSkyLightUpdate[dir.getOpposite().ordinal()] = false;
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
                    cube.getCubicWorld().getLightingManager().relightMultiBlock(
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMinBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMaxBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            EnumSkyBlock.SKY, pos -> {
                                cube.getCubicWorld().notifyLightSet(pos);
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
            return hasUpdates;
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
