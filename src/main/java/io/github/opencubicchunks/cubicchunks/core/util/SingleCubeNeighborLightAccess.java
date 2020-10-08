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

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

// based on https://github.com/bergerhealer/Light-Cleaner/blob/9e7a020c0564f4d74dccc54003e9a08f1ff52f6d/src/main/java/com/bergerkiller/bukkit/lightcleaner/lighting/LightingCubeNeighboring.java
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SingleCubeNeighborLightAccess implements ILightBlockAccess, IBlockAccess {

    private final ExtendedBlockStorage[] storageArray = new ExtendedBlockStorage[6];
    private final Cube[] cubeArray = new Cube[6];
    private final Chunk[] columnArray = new Chunk[4];
    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;
    private final Cube centerCube;
    private ExtendedBlockStorage centerStorage;
    private final Chunk centerColumn;

    private final WorldType worldType;

    public SingleCubeNeighborLightAccess(ICube cube) {
        int x = cube.getX();
        int y = cube.getY();
        int z = cube.getZ();

        for (EnumFacing value : EnumFacing.VALUES) {
            int offX = value.getXOffset();
            int x1 = x + offX;
            int offY = value.getYOffset();
            int y1 = y + offY;
            int offZ = value.getZOffset();
            int z1 = z + offZ;
            int idx = getIndexByCube(offX, offY, offZ);
            ICube offsetCube = cube.getWorld().getCubeCache().getLoadedCube(x1, y1, z1);
            if (offsetCube == null || !offsetCube.isInitialLightingDone()) {
                continue;
            }
            cubeArray[idx] = (Cube) offsetCube;
            storageArray[idx] = offsetCube.getStorage();
            columnArray[getIndexByColumn(offX, offZ)] = offsetCube.getColumn();
        }
        this.cubeX = x;
        this.cubeY = y;
        this.cubeZ = z;
        this.centerCube = (Cube) cube;
        this.centerColumn = cube.getColumn();
        this.centerStorage = cube.getStorage();
        // for IBlockAccess
        this.worldType = cube.getWorld().getWorldInfo().getTerrainType();
    }

    /**
     * Generates a key ranging 0 - 5 for fixed x/y/z combinations<br>
     * - Bit 1 is set to contain whether x/y/z is 1 or -1
     * - Bit 2 is set to 1 when the axis is x<br>
     * - Bit 3 is set to 1 when the axis is z<br><br>
     * <p/>
     * This system requires that the x/y/z pairs are one the following:<br>
     * (0, 0, 1) | (0, 0, -1) | (0, 1, 0) | (0, -1, 0) | (1, 0, 0) | (-1, 0, 0)
     *
     * @param x value
     * @param y value
     * @param z value
     * @return key
     */
    private static int getIndexByCube(int x, int y, int z) {
        return (((x + y + z + 1) & 0x2) >> 1) | ((x & 0x1) << 1) | ((z & 0x1) << 2);
    }

    /**
     * Generates a key ranging 0 - 3 for fixed x/z combinations<br>
     * - Bit 1 is set to contain which of the two is not 1<br>
     * - Bit 2 is set to contain whether x/z is 1 or -1<br><br>
     * <p/>
     * This system requires that the x/z pairs are one the following:<br>
     * (0, 1) | (0, -1) | (1, 0) | (-1, 0)
     *
     * @param x value
     * @param z value
     * @return key
     */
    private static int getIndexByColumn(int x, int z) {
        return (x & 1) | ((x + z + 1) & 0x2);
    }

    // ILightBlockAccess, fast path

    @Override public int getBlockLightOpacity(BlockPos pos) {
        int dx = blockToCube(pos.getX()) - cubeX;
        int dy = blockToCube(pos.getY()) - cubeY;
        int dz = blockToCube(pos.getZ()) - cubeZ;
        ExtendedBlockStorage storage;
        if ((dx | dy | dz) == 0) {
            storage = centerStorage;
        } else {
            storage = storageArray[getIndexByCube(dx, dy, dz)];
        }
        return storage == null ? 0 : storage.get(
                blockToLocal(pos.getX()),
                blockToLocal(pos.getY()),
                blockToLocal(pos.getZ())
        ).getLightOpacity(this, pos);
    }

    @Override public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        int dx = blockToCube(pos.getX()) - cubeX;
        int dy = blockToCube(pos.getY()) - cubeY;
        int dz = blockToCube(pos.getZ()) - cubeZ;
        int x = blockToLocal(pos.getX());
        int y = blockToLocal(pos.getY());
        int z = blockToLocal(pos.getZ());

        ExtendedBlockStorage storage;
        if ((dx | dy | dz) == 0) {
            storage = centerStorage;
        } else {
            int indexByCube = getIndexByCube(dx, dy, dz);
            storage = storageArray[indexByCube];
        }
        if (storage == null) {
            return 0;
        }
        if (lightType == EnumSkyBlock.BLOCK) {
            return storage.getBlockLight(x, y, z);
        } else {
            return storage.getSkyLight(x, y, z);
        }
    }

    @Override public boolean setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
        int blockX = pos.getX();
        int x = blockToCube(blockX);
        int blockY = pos.getY();
        int y = blockToCube(blockY);
        int blockZ = pos.getZ();
        int z = blockToCube(blockZ);
        if (cubeX != x || cubeY != y || cubeZ != z) {
            return false;
        }
        ExtendedBlockStorage storage = centerStorage;
        if (storage == null) {
            Cube cube = centerCube;
            storage = new ExtendedBlockStorage(cubeToMinBlock(cube.getY()), cube.getWorld().provider.hasSkyLight());
            cube.setStorage(storage);
            centerStorage = storage;
        }
        int xLocal = blockToLocal(pos.getX());
        int yLocal = blockToLocal(pos.getY());
        int zLocal = blockToLocal(pos.getZ());
        if (lightType == EnumSkyBlock.SKY) {
            storage.setSkyLight(xLocal, yLocal, zLocal, val);
        } else {
            storage.setBlockLight(xLocal, yLocal, zLocal, val);
        }
        return true;
    }

    @Override public boolean canSeeSky(BlockPos pos) {
        int blockX = pos.getX();
        int blockZ = pos.getZ();
        int dx = blockToCube(blockX) - cubeX;
        int dz = blockToCube(blockZ) - cubeZ;
        Chunk chunk;
        if ((dx | dz) == 0) {
            chunk = centerColumn;
        } else {
            chunk = columnArray[getIndexByColumn(dx, dz)];
            if (chunk == null) {
                return false;
            }
        }
        int height = ((IColumnInternal) chunk).getHeightWithStaging(
                blockToLocal(blockX),
                blockToLocal(blockZ)
        );
        return pos.getY() >= height;
    }

    @Override public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
        if (type == EnumSkyBlock.BLOCK) {
            return getBlockState(pos).getLightValue(this, pos);
        } else {
            return canSeeSky(pos) ? 15 : 0;
        }
    }

    @Override public void markEdgeNeedLightUpdate(BlockPos pos, EnumSkyBlock type) {
        if (type == EnumSkyBlock.BLOCK)
            return;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (blockToCube(x) != cubeX || blockToCube(y) != cubeY || blockToCube(z) != cubeZ) {
            return;
        }
        Cube cube = centerCube;
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

    @Override public boolean hasNeighborsAccessible(BlockPos pos) {
        return cubeX == blockToCube(pos.getX()) && cubeY == blockToCube(pos.getY()) && cubeZ == blockToCube(pos.getZ());
    }

    // IBlockAccess

    @Nullable @Override public TileEntity getTileEntity(BlockPos pos) {
        int dx = blockToCube(pos.getX()) - cubeX;
        int dy = blockToCube(pos.getY()) - cubeY;
        int dz = blockToCube(pos.getZ()) - cubeZ;
        Cube cube;
        if ((dx | dy | dz) == 0) {
            cube = centerCube;
        } else {
            cube = cubeArray[getIndexByCube(dx, dy, dz)];
            if (cube == null) {
                return null;
            }
        }
        return cube.getTileEntityMap().get(pos);
    }

    @Override public int getCombinedLight(BlockPos pos, int lightValue) {
        int skyLight = this.getLightFor(EnumSkyBlock.SKY, pos);
        int blockLight = this.getLightFor(EnumSkyBlock.BLOCK, pos);

        if (blockLight < lightValue) {
            blockLight = lightValue;
        }
        return skyLight << 20 | blockLight << 4;
    }

    @Override public IBlockState getBlockState(BlockPos pos) {
        int dx = blockToCube(pos.getX()) - cubeX;
        int dy = blockToCube(pos.getY()) - cubeY;
        int dz = blockToCube(pos.getZ()) - cubeZ;
        ExtendedBlockStorage storage;
        if ((dx | dy | dz) == 0) {
            storage = centerStorage;
        } else {
            storage = storageArray[getIndexByCube(dx, dy, dz)];
        }
        if (storage == null) {
            return Blocks.AIR.getDefaultState();
        }
        return storage.get(blockToLocal(pos.getX()), blockToLocal(pos.getY()), blockToLocal(pos.getZ()));
    }

    @Override public boolean isAirBlock(BlockPos pos) {
        return getBlockState(pos).getBlock() == Blocks.AIR;
    }

    @Override public Biome getBiome(BlockPos pos) {
        int blockX = pos.getX();
        int blockZ = pos.getZ();
        int dx = blockToCube(blockX) - cubeX;
        int dz = blockToCube(blockZ) - cubeZ;
        Chunk chunk;
        if ((dx | dz) == 0) {
            chunk = centerColumn;
        } else {
            chunk = columnArray[getIndexByColumn(dx, dz)];
            if (chunk == null) {
                return Biomes.PLAINS;
            }
        }
        // TODO: 3d biomes?
        return chunk.getBiome(pos, chunk.getWorld().getBiomeProvider());
    }

    @Override public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override public WorldType getWorldType() {
        return worldType;
    }

    @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        int dx = blockToCube(pos.getX()) - cubeX;
        int dy = blockToCube(pos.getY()) - cubeY;
        int dz = blockToCube(pos.getZ()) - cubeZ;
        ExtendedBlockStorage storage;
        if ((dx | dy | dz) == 0) {
            storage = centerStorage;
        } else {
            storage = storageArray[getIndexByCube(dx, dy, dz)];
        }
        if (storage == null) {
            return _default;
        }
        IBlockState state = storage.get(blockToLocal(pos.getX()), blockToLocal(pos.getY()), blockToLocal(pos.getZ()));
        return state.getBlock().isSideSolid(state, this, pos, side);
    }
}
