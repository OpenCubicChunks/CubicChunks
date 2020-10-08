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
package io.github.opencubicchunks.cubicchunks.core.util;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Simple class that allows to quickly access blocks near specified cube without the overhead of getting these cubes.
 * <p>
 * Does not allow to set blocks, only get blocks, their opacity and get/set light values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FastCubeBlockAccess implements ILightBlockAccess {

    @Nonnull private final ExtendedBlockStorage[][][] cache;
    @Nonnull private final Cube[][][] cubes;
    @Nonnull private final Chunk[][] columns;
    private final int originX, originY, originZ;
    private final int dx, dy, dz;
    @Nonnull private final World world;

    public FastCubeBlockAccess(ICubeProviderInternal cache, ICube cube, int radius) {
        this(cube.getWorld(), cache,
                cube.getCoords().sub(radius, radius, radius), cube.getCoords().add(radius, radius, radius));
    }

    private FastCubeBlockAccess(World world, ICubeProviderInternal prov, CubePos start, CubePos end) {
        this.dx = Math.abs(end.getX() - start.getX()) + 1;
        this.dy = Math.abs(end.getY() - start.getY()) + 1;
        this.dz = Math.abs(end.getZ() - start.getZ()) + 1;

        this.world = world;
        this.cache = new ExtendedBlockStorage[dx][dy][dz];
        this.cubes = new Cube[dx][dy][dz];
        this.columns = new Chunk[dx][dz];
        this.originX = Math.min(start.getX(), end.getX());
        this.originY = Math.min(start.getY(), end.getY());
        this.originZ = Math.min(start.getZ(), end.getZ());

        for (int relativeCubeX = 0; relativeCubeX < dx; relativeCubeX++) {
            for (int relativeCubeZ = 0; relativeCubeZ < dz; relativeCubeZ++) {
                this.columns[relativeCubeX][relativeCubeZ] = prov.getLoadedColumn(originX + relativeCubeX, originZ + relativeCubeZ);
                for (int relativeCubeY = 0; relativeCubeY < dy; relativeCubeY++) {
                    Cube cube = prov.getLoadedCube(originX + relativeCubeX, originY + relativeCubeY, originZ + relativeCubeZ);
                    if (cube != null) {
                        ExtendedBlockStorage storage = cube.getStorage();
                        this.cache[relativeCubeX][relativeCubeY][relativeCubeZ] = storage;
                        this.cubes[relativeCubeX][relativeCubeY][relativeCubeZ] = cube;
                        // markDirty ahead of time to avoid doing it on every setLight
                        cube.markDirty();
                    }
                }
            }
        }
    }

    @Nullable
    private ExtendedBlockStorage getStorage(int blockX, int blockY, int blockZ) {
        int cubeX = Coords.blockToCube(blockX);
        int cubeY = Coords.blockToCube(blockY);
        int cubeZ = Coords.blockToCube(blockZ);
        if (cubeX < originX || cubeY < originY || cubeZ < originZ)
            return null;
        cubeX -= originX;
        cubeY -= originY;
        cubeZ -= originZ;
        if (cubeX >= dx || cubeY >= dy || cubeZ >= dz)
            return null;
        return this.cache[cubeX][cubeY][cubeZ];
    }

    private void setStorage(int blockX, int blockY, int blockZ, @Nullable ExtendedBlockStorage ebs) {
        int cubeX = Coords.blockToCube(blockX);
        int cubeY = Coords.blockToCube(blockY);
        int cubeZ = Coords.blockToCube(blockZ);

        this.cache[cubeX - originX][cubeY - originY][cubeZ - originZ] = ebs;
    }

    @Nullable
    private Cube getCube(int blockX, int blockY, int blockZ) {
        int cubeX = Coords.blockToCube(blockX);
        int cubeY = Coords.blockToCube(blockY);
        int cubeZ = Coords.blockToCube(blockZ);
        if (cubeX < originX || cubeY < originY || cubeZ < originZ)
            return null;
        cubeX -= originX;
        cubeY -= originY;
        cubeZ -= originZ;
        if (cubeX >= dx || cubeY >= dy || cubeZ >= dz)
            return null;
        return this.cubes[cubeX][cubeY][cubeZ];
    }

    private IBlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    private IBlockState getBlockState(int blockX, int blockY, int blockZ) {
        ExtendedBlockStorage ebs = this.getStorage(blockX, blockY, blockZ);
        if (ebs != null) {
            return ebs.get(blockToLocal(blockX), blockToLocal(blockY), blockToLocal(blockZ));
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public int getBlockLightOpacity(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ()).getLightOpacity(world, pos);
    }

    @Override 
    public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        ExtendedBlockStorage ebs = this.getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (ebs != null) {
            int localX = blockToLocal(pos.getX());
            int localY = blockToLocal(pos.getY());
            int localZ = blockToLocal(pos.getZ());

            if (lightType == EnumSkyBlock.SKY) {
                return ebs.getSkyLight(localX, localY, localZ);
            } else {
                return ebs.getBlockLight(localX, localY, localZ);
            }
        }
        return 0;
    }

    @Override 
    public boolean setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
        ExtendedBlockStorage ebs = this.getStorage(pos.getX(), pos.getY(), pos.getZ());
        if (ebs != null) {
            int localX = blockToLocal(pos.getX());
            int localY = blockToLocal(pos.getY());
            int localZ = blockToLocal(pos.getZ());

            if (lightType == EnumSkyBlock.SKY) {
                ebs.setSkyLight(localX, localY, localZ, val);
            } else {
                ebs.setBlockLight(localX, localY, localZ, val);
            }
            return true;
        }
        Cube cube = getCube(pos.getX(), pos.getY(), pos.getZ());
        if (cube != null) {
            cube.setLightFor(lightType, pos, val);
            setStorage(pos.getX(), pos.getY(), pos.getZ(), cube.getStorage());
            return true;
        }
        return false;
    }

    @Override public boolean canSeeSky(BlockPos pos) {
        int blockX = pos.getX();
        int blockY = pos.getY();
        int blockZ = pos.getZ();
        int cubeX = Coords.blockToCube(blockX);
        int cubeZ = Coords.blockToCube(blockZ);
        if (cubeX < originX || cubeZ < originZ)
            return false;
        cubeX -= originX;
        cubeZ -= originZ;
        if (cubeX >= dx || cubeZ >= dz)
            return false;
        Chunk column = columns[cubeX][cubeZ];
        if (column == null)
            return false;
        int height = ((IColumnInternal) column).getHeightWithStaging(blockToLocal(blockX), blockToLocal(blockZ));
        return height <= blockY;
    }

    @Override public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
        switch (type) {
            case BLOCK:
                return getBlockState(pos).getLightValue(world, pos);
            case SKY:
                return canSeeSky(pos) ? 15 : 0;
            default:
                throw new AssertionError();
        }
    }

    public static ILightBlockAccess forBlockRegion(ICubeProviderInternal prov, BlockPos startPos, BlockPos endPos) {
        //TODO: fix it
        BlockPos midPos = Coords.midPos(startPos, endPos);
        Cube center = prov.getCube(CubePos.fromBlockCoords(midPos));
        return new FastCubeBlockAccess(center.getWorld(), prov,
                CubePos.fromBlockCoords(startPos), CubePos.fromBlockCoords(endPos));
    }

    @Override
    public void markEdgeNeedLightUpdate(BlockPos pos, EnumSkyBlock type) {
        if (type == EnumSkyBlock.BLOCK)
            return;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Cube cube = this.getCube(x, y, z);
        if (cube == null)
            return;
        // What edge?
        int localX = Coords.blockToLocal(x);
        int localY = Coords.blockToLocal(y);
        int localZ = Coords.blockToLocal(z);
        if (localX == 0) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.WEST);
        } else if (localX == 15) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.EAST);
        }
        if (localY == 0) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.DOWN);
        } else if (localY == 15) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.UP);
        }
        if (localZ == 0) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.NORTH);
        } else if (localZ == 15) {
            cube.markEdgeNeedSkyLightUpdate(EnumFacing.SOUTH);
        }
    }
}
