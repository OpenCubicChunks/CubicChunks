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

import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.util.FastCubeBlockAccess;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: extract interfaces when it's done
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager {

    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
    @Nonnull private ICubicWorld world;
    @Nonnull private LightPropagator lightPropagator = new LightPropagator();
    @Nonnull private final List<IHeightChangeListener> heightUpdateListeners = new ArrayList<>();

    public LightingManager(ICubicWorld world) {
        this.world = world;
    }

    /**
     * Registers height change listener, that receives all height changes after initial lighting is done
     */
    public void registerHeightChangeListener(IHeightChangeListener listener) {
        heightUpdateListeners.add(listener);
    }

    @Nullable
    public CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
        if (cube.getCubicWorld().getProvider().hasNoSky()) {
            return null;
        }
        return new CubeLightUpdateInfo(cube);
    }

    private void columnSkylightUpdate(UpdateType type, IColumn column, int localX, int minY, int maxY, int localZ) {
        if (world.getProvider().hasNoSky()) {
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
                new BlockPos(blockX, minInCubeY, blockZ), new BlockPos(blockX, maxInCubeY, blockZ), EnumSkyBlock.SKY);
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
    boolean relightMultiBlock(BlockPos startPos, BlockPos endPos, EnumSkyBlock type) {
        // TODO: optimize if needed

        // TODO: Figure out why it crashes with value 17
        final int LOAD_RADIUS = 31;
        BlockPos midPos = Coords.midPos(startPos, endPos);
        BlockPos minLoad = startPos.add(-LOAD_RADIUS, -LOAD_RADIUS, -LOAD_RADIUS);
        BlockPos maxLoad = endPos.add(LOAD_RADIUS, LOAD_RADIUS, LOAD_RADIUS);

        if (!world.testForCubes(CubePos.fromBlockCoords(minLoad), CubePos.fromBlockCoords(maxLoad),
                c -> c != null && !(c instanceof BlankCube))) {
            return false;
        }
        ILightBlockAccess blocks = FastCubeBlockAccess.forBlockRegion(world.getCubeCache(), minLoad, maxLoad);
        this.lightPropagator.propagateLight(midPos, BlockPos.getAllInBox(startPos, endPos), blocks, type, world::notifyLightSet);
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
            if (!this.hasUpdates) {
                return;
            }
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    if (!toUpdateColumns[index(localX, localZ)]) {
                        continue;
                    }
                    boolean success = cube.getCubicWorld().getLightingManager().relightMultiBlock(
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMinBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            new BlockPos(localToBlock(cube.getX(), localX), cubeToMaxBlock(cube.getY()), localToBlock(cube.getZ(), localZ)),
                            EnumSkyBlock.SKY
                    );
                    if (!success) {
                        return;
                    }
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
