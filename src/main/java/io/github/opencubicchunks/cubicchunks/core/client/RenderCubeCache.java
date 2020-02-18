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
package io.github.opencubicchunks.cubicchunks.core.client;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RenderCubeCache extends ChunkCache {

    protected int cubeY;
    @Nonnull private final ExtendedBlockStorage[][][] cubeArrays;
    @Nonnull private final Map<BlockPos, TileEntity>[][][] tileEntities;

    @Nonnull private final World world;

    public RenderCubeCache(World world, BlockPos from, BlockPos to, int subtract) {
        super(world, from, to, subtract);
        this.world = world;
        this.cubeY = Coords.blockToCube(from.getY() - subtract);
        int cubeXEnd = Coords.blockToCube(to.getX() + subtract);
        int cubeYEnd = Coords.blockToCube(to.getY() + subtract);
        int cubeZEnd = Coords.blockToCube(to.getZ() + subtract);

        cubeArrays = new ExtendedBlockStorage[cubeXEnd - this.chunkX + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.chunkZ + 1];
        // because java is stupid and won't allow generic array creation, and temporary local variable because it won't allow annotation on assignment
        @SuppressWarnings("unchecked")
        Map<BlockPos, TileEntity>[][][] tileEntities = new Map[cubeXEnd - this.chunkX + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.chunkZ + 1];
        this.tileEntities = tileEntities;

        ExtendedBlockStorage nullStorage = new ExtendedBlockStorage(0, true);

        for (int currentCubeX = chunkX; currentCubeX <= cubeXEnd; currentCubeX++) {
            for (int currentCubeY = cubeY; currentCubeY <= cubeYEnd; currentCubeY++) {
                for (int currentCubeZ = chunkZ; currentCubeZ <= cubeZEnd; currentCubeZ++) {
                    ExtendedBlockStorage ebs;
                    Map<BlockPos, TileEntity> teMap;

                    Cube cube = ((ICubicWorldInternal) world).getCubeFromCubeCoords(currentCubeX, currentCubeY, currentCubeZ);
                    ebs = cube.getStorage();

                    teMap = cube.getTileEntityMap();

                    if (ebs == null) {
                        ebs = nullStorage;
                    }
                    cubeArrays[currentCubeX - chunkX][currentCubeY - cubeY][currentCubeZ - chunkZ] = ebs;
                    tileEntities[currentCubeX - chunkX][currentCubeY - cubeY][currentCubeZ - chunkZ] = teMap;
                }
            }
        }
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int blockLight = this.getLightForExt(EnumSkyBlock.SKY, pos);
        int skyLight = this.getLightForExt(EnumSkyBlock.BLOCK, pos);

        if (skyLight < lightValue) {
            skyLight = lightValue;
        }

        return blockLight << 20 | skyLight << 4;
    }

    @Override
    @Nullable public TileEntity getTileEntity(BlockPos pos) {
        int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
        int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
        int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
        if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
                arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
                arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
            return null;
        }
        return this.tileEntities[arrayX][arrayY][arrayZ].get(pos);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        if (world.isOutsideBuildHeight(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
        int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
        int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;

        if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
                arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
                arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
            return Blocks.AIR.getDefaultState();
        }
        return this.cubeArrays[arrayX][arrayY][arrayZ].get(blockToLocal(pos.getX()), blockToLocal(pos.getY()), blockToLocal(pos.getZ()));
    }

    private int getLightForExt(EnumSkyBlock type, BlockPos pos) {
        if (type == EnumSkyBlock.SKY && !this.world.provider.hasSkyLight()) {
            return 0;
        }
        if (world.isOutsideBuildHeight(pos)) {
            return type.defaultLightValue;
        }
        if (this.getBlockState(pos).useNeighborBrightness()) {
            int max = 0;

            for (EnumFacing enumfacing : EnumFacing.values()) {
                int current = this.getLightFor(type, pos.offset(enumfacing));
                if (current > max) {
                    max = current;
                }
                if (max >= 15) {
                    return max;
                }
            }
            return max;
        }
        int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
        int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
        int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
        if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
                arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
                arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
            return type.defaultLightValue;
        }
        ExtendedBlockStorage cube = this.cubeArrays[arrayX][arrayY][arrayZ];
        return getRawLight(cube, type, pos);
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        if (world.isOutsideBuildHeight(pos)) {
            return type.defaultLightValue;
        }
        int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
        int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
        int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
        if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
                arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
                arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
            return type.defaultLightValue;
        }
        ExtendedBlockStorage cube = this.cubeArrays[arrayX][arrayY][arrayZ];
        return getRawLight(cube, type, pos);
    }

    private int getRawLight(ExtendedBlockStorage ebs, EnumSkyBlock type, BlockPos pos) {
        if (type == EnumSkyBlock.BLOCK) {
            return ebs.getBlockLight(blockToLocal(pos.getX()), blockToLocal(pos.getY()), blockToLocal(pos.getZ()));
        } else {
            return ebs.getSkyLight(blockToLocal(pos.getX()), blockToLocal(pos.getY()), blockToLocal(pos.getZ()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        if (world.isOutsideBuildHeight(pos)) {
            return defaultValue;
        }
        int arrayX = Coords.blockToCube(pos.getX()) - this.chunkX;
        int arrayY = Coords.blockToCube(pos.getY()) - this.cubeY;
        int arrayZ = Coords.blockToCube(pos.getZ()) - this.chunkZ;
        if (arrayX < 0 || arrayX >= this.cubeArrays.length ||
                arrayY < 0 || arrayY >= this.cubeArrays[arrayX].length ||
                arrayZ < 0 || arrayZ >= this.cubeArrays[arrayX][arrayY].length) {
            return defaultValue;
        }
        IBlockState state = getBlockState(pos);
        return state.getBlock().isSideSolid(state, this, pos, side);
    }
}
