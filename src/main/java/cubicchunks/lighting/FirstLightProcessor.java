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

import static cubicchunks.util.Coords.cubeToMinBlock;
import cubicchunks.CubicChunks;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import javax.annotation.Nonnull;
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

    public boolean debugMode = false;
    private byte[] opacityView = new byte[4096];
    private byte[] lightView = new byte[4096];
    private int[][] indexs = new int[14][4096];
    private int[] indexsSize = new int[14];

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
    private void initializeSkylight(Cube cube) {
        for (int i = 0; i < this.lightView.length; i++) {
            this.lightView[i] = (byte) 0;
            this.opacityView[i] = (byte) 0;
        }
        for (int pass = 0; pass < this.indexsSize.length; pass++) {
            this.indexsSize[pass] = 0;
        }
        IHeightMap opacityIndex = cube.getColumn().getOpacityIndex();
        CubePos cpos = cube.getCoords();
        BlockPos startPos = cpos.getMinBlockPos();
        int sx = startPos.getX();
        int sy = startPos.getY();
        int sz = startPos.getZ();
        int cubeMinY = cubeToMinBlock(cube.getY());
        for (int localX = 0; localX < Cube.SIZE; ++localX) {
            for (int localZ = 0; localZ < Cube.SIZE; ++localZ) {
                for (int localY = Cube.SIZE - 1; localY >= 0; --localY) {
                    if (opacityIndex.isOccluded(localX, cubeMinY + localY, localZ)) {
                        break;
                    }
                    int index = getIndex(localX, localY, localZ);
                    this.lightView[index] = 15;
                    indexs[0][indexsSize[0]] = index;
                    if (debugMode)
                        CubicChunks.LOGGER.info("Adding #" + indexsSize[0] + " for index " + index + " at {" + (sx + localX) + ";" + (sy + localY)
                                + ";" + (sz + localZ) + "}");
                    indexsSize[0]++;
                }
            }
        }
        for (EnumFacing dir : EnumFacing.values()) {
            Cube loadedCube = cache.getLoadedCube(
                    cpos.getX() + dir.getFrontOffsetX(),
                    cpos.getY() + dir.getFrontOffsetY(),
                    cpos.getZ() + dir.getFrontOffsetZ());
            if (loadedCube == null || !loadedCube.isInitialLightingDone()) {
                cube.edgeNeedSkyLightUpdate[dir.ordinal()] = true;
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
                    toBlockY = fromBlockY;
                    break;
                case UP:
                    toBlockY = toBlockY + 1;
                    fromBlockY = toBlockY;
                    break;
                case NORTH:
                    fromBlockZ = fromBlockZ - 1;
                    toBlockZ = fromBlockZ;
                    break;
                case SOUTH:
                    toBlockZ = toBlockZ + 1;
                    fromBlockZ = toBlockZ;
                    break;
                case WEST:
                    fromBlockX = fromBlockX - 1;
                    toBlockX = fromBlockX;
                    break;
                case EAST:
                    toBlockX = toBlockX + 1;
                    fromBlockX = toBlockX;
                    break;
            }
            for (int x = fromBlockX; x <= toBlockX; x++) {
                for (int y = fromBlockY; y <= toBlockY; y++) {
                    for (int z = fromBlockZ; z <= toBlockZ; z++) {
                        int lightValue = cube.getStorage().getSkyLight(Coords.blockToLocal(x), Coords.blockToLocal(y), Coords.blockToLocal(z)) - 1;
                        if (lightValue > 1) {
                            int lx = Coords.blockToLocal(x - dir.getFrontOffsetX());
                            int ly = Coords.blockToLocal(y - dir.getFrontOffsetY());
                            int lz = Coords.blockToLocal(z - dir.getFrontOffsetZ());
                            int index = getIndex(lx, ly, lz);
                            int oldLightValue = this.lightView[index];
                            if (lightValue <= oldLightValue)
                                continue;
                            int pass = 15 - lightValue;
                            this.lightView[index] = (byte) lightValue;
                            indexs[pass][indexsSize[pass]] = index;
                            indexsSize[pass]++;
                        }
                    }
                }
            }
        }
    }

    private void fillOpacityView(IBlockAccess world, BlockPos startPos, ExtendedBlockStorage storage) {
        if (storage == null)
            return;
        int sx = startPos.getX();
        int sy = startPos.getY();
        int sz = startPos.getZ();
        for (int i = 0; i < this.opacityView.length; i++) {
            int ly = i >> 8;
            int lz = (i >> 4) & 15;
            int lx = i & 15;
            mutablePos.setPos(sx + lx, sy + ly, sz + lz);
            this.opacityView[i] = (byte) (storage.get(lx, ly, lz).getLightOpacity(world, mutablePos) >>> 4);
        }
    }

    private static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
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
        BlockPos startPos = cube.getCoords().getMinBlockPos();
        int sx = startPos.getX();
        int sy = startPos.getY();
        int sz = startPos.getZ();
        if (cube.getStorage() == null) {
            cube.setStorage(new ExtendedBlockStorage(sy, true));
        }
        this.initializeSkylight(cube);
        this.fillOpacityView((IBlockAccess) cube.getCubicWorld(), startPos, cube.getStorage());
        int[] sides = new int[] {0, 0, -1, 0, 0, 1, 0, 0};
        for (int pass = 0; pass < this.indexsSize.length; pass++) {
            if (debugMode)
                CubicChunks.LOGGER.info("Pass:" + pass);
            for (int i = 0; i < this.indexsSize[pass]; i++) {
                int index = this.indexs[pass][i];
                int ly = index >> 8;
                int lz = (index >> 4) & 15;
                int lx = index & 15;
                if (debugMode)
                    CubicChunks.LOGGER
                            .info("Checking arounds #" + i + " for index " + index + " at {" + (sx + lx) + ";" + (sy + ly) + ";" + (sz + lz) + "}");
                for (int i1 = 2; i1 < sides.length; i1++) {
                    int lx1 = lx + sides[i1 - 2];
                    int ly1 = ly + sides[i1 - 1];
                    int lz1 = lz + sides[i1];
                    if (lx1 < 0 || lx1 > 15 || ly1 < 0 || ly1 > 15 || lz1 < 0 || lz1 > 15)
                        continue;
                    int lightValue = 14 - pass;
                    int index1 = getIndex(lx1, ly1, lz1);
                    int oldLightValue = this.lightView[index1];
                    if (lightValue <= oldLightValue)
                        continue;
                    int opacity = this.opacityView[index1];
                    if (opacity == 15)
                        continue;
                    this.lightView[index1] = (byte) lightValue;
                    int nextPass = pass + opacity + 1;
                    if (nextPass >= 14)
                        continue;
                    indexs[nextPass][indexsSize[nextPass]] = index1;
                    indexsSize[nextPass]++;
                }
            }
        }
        for (int index = 0; index < this.lightView.length; index++) {
            int ly = index >> 8;
            int lz = (index >> 4) & 15;
            int lx = index & 15;
            mutablePos.setPos(sx + lx, sy + ly, sz + lz);
            if (this.lightView[index] == 0)
                continue;
            cube.getStorage().setSkyLight(lx, ly, lz, this.lightView[index]);
            tracker.onUpdate(mutablePos);
        }
        tracker.sendAll();
        cube.setInitialLightingDone(true);
    }
}
