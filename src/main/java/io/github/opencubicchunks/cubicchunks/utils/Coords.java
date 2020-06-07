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

package io.github.opencubicchunks.cubicchunks.utils;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Coords {

    public static final int NO_HEIGHT = Integer.MIN_VALUE + 32;

    private static final int LOG2_BLOCK_SIZE = MathUtil.log2(ICube.BLOCK_SIZE);

    private static final int BLOCK_SIZE_MINUS_1 = ICube.BLOCK_SIZE - 1;
    private static final int BLOCK_SIZE_DIV_2 = ICube.BLOCK_SIZE / 2;
    private static final int BLOCK_SIZE_DIV_16 = ICube.BLOCK_SIZE / 16;
    private static final int BLOCK_SIZE_DIV_32 = ICube.BLOCK_SIZE / 32;

    private static final int POS_TO_INDEX_MASK = getPosToIndexMask();
    private static final int INDEX_TO_POS_MASK = POS_TO_INDEX_MASK >> 4;

    private static final int INDEX_TO_N_X = 0;
    private static final int INDEX_TO_N_Y = LOG2_BLOCK_SIZE - 4;
    private static final int INDEX_TO_N_Z = INDEX_TO_N_Y * 2;



    private static int getPosToIndexMask()
    {
        int mask = 0;
        for(int i = ICube.BLOCK_SIZE/2; i >= 16; i /= 2)
        {
            mask += i;
        }
        return mask;
    }

    public static BlockPos midPos(BlockPos p1, BlockPos p2) {
        //bitshifting each number and then adding the result - this rounds the number down and prevents overflow
        return new BlockPos((p1.getX() >> 1) + (p2.getX() >> 1) + (p1.getX() & p2.getX() & 1),
                (p1.getY() >> 1) + (p2.getY() >> 1) + (p1.getY() & p2.getY() & 1),
                (p1.getZ() >> 1) + (p2.getZ() >> 1) + (p1.getZ() & p2.getZ() & 1));
    }

    public static int blockToLocal(int val) {
        return val & (ICube.BLOCK_SIZE - 1);
    }

    public static int localX(BlockPos pos) {
        return blockToLocal(pos.getX());
    }

    public static int localY(BlockPos pos) {
        return blockToLocal(pos.getY());
    }

    public static int localZ(BlockPos pos) {
        return blockToLocal(pos.getZ());
    }

    public static int blockToCube(int val) {
        return val >> LOG2_BLOCK_SIZE;
    }

    public static int blockCeilToCube(int val) {
        return -((-val) >> LOG2_BLOCK_SIZE);
    }

    public static int localToBlock(int cubeVal, int localVal) {
        return cubeToMinBlock(cubeVal) + localVal;
    }

    public static int cubeToMinBlock(int val) {
        return val << LOG2_BLOCK_SIZE;
    }

    public static int cubeToMaxBlock(int val) {
        return cubeToMinBlock(val) + BLOCK_SIZE_MINUS_1;
    }

    public static int getCubeXForEntity(Entity entity) {
        return blockToCube(MathHelper.floor(entity.getPosX()));
    }

    public static int getCubeZForEntity(Entity entity) {
        return blockToCube(MathHelper.floor(entity.getPosZ()));
    }

    public static int getCubeYForEntity(Entity entity) {
        // the entity is in the cube it's inside, not the cube it's standing on
        return blockToCube(MathHelper.floor(entity.getPosY()));
    }

    public static int blockToCube(double blockPos) {
        return blockToCube(MathHelper.floor(blockPos));
    }

    public static int cubeToCenterBlock(int cubeVal) {
        return localToBlock(cubeVal, BLOCK_SIZE_DIV_2);
    }

    public static int blockToIndex(int x, int y, int z) {
        //        Given x pos 33 = 0x21 = 0b0100001
        //        1100000
        //        0b0100001 & 0b1100000 = 0b0100000
        //        0b0100000 >> 4 = 0b10 = 0x2 = 2

        //        Given y pos 532 = 0x214 = 0b1000010100
        //        0b0001100000
        //        0b0001100000
        //        0b1000010100 & 0b0001100000 = 0b0
        //        0b0 >> 4 = 0b0 = 0x0 = 0

        //        Given z pos -921 = -0x399 = 0b1110011001
        //        0b0001100000
        //        0b0001100000
        //        0b1000010100 & 0b0001100000 = 0b0
        //        0b0 >> 4 = 0b0 = 0x0 = 0

        //        mask needs to be every power of 2 below ICube.BLOCK_SIZE that's > 16

        if(ICube.CUBE_DIAMETER == 1) {
            return blockToIndex16(x, y, z);
        }
        else if(ICube.CUBE_DIAMETER == 2) {
            return blockToIndex32(x, y, z);
        }
        else if(ICube.CUBE_DIAMETER == 4) {
            return blockToIndex64(x, y, z);
        }
        else if(ICube.CUBE_DIAMETER == 8) {
            return blockToIndex128(x, y, z);
        }
        throw new UnsupportedOperationException("Unsupported cube size " + ICube.CUBE_DIAMETER);
    }

    private static int blockToIndex16(int x, int y, int z)
    {
        return 0;
    }

    private static int blockToIndex32(int x, int y, int z)
    {
        //1 bit
        final int mask = POS_TO_INDEX_MASK;
        return (x&mask) >> 4 | (y&mask) >> 3 | (z&mask) >> 2;
    }

    private static int blockToIndex64(int x, int y, int z)
    {
        //2 bit
        //1011101010001, 1010101011100, 1101011101010
        final int mask = POS_TO_INDEX_MASK;
        return (x&mask) >> 4 | (y&mask) >> 2 | (z&mask) >> 0;
    }

    private static int blockToIndex128(int x, int y, int z)
    {
        //3 bit
        //1011101010001, 1010101011100, 1101011101010
        final int mask = POS_TO_INDEX_MASK;
        return (x&mask) >> 4 | (y&mask) >> 1 | (z&mask) << 2;
    }

    public static int indexToX(int idx) {
        return idx >> INDEX_TO_N_X & INDEX_TO_POS_MASK;
    }

    public static int indexToY(int idx) {
            return idx >> INDEX_TO_N_Y & INDEX_TO_POS_MASK;
    }

    public static int indexToZ(int idx) {
        return idx >> INDEX_TO_N_Z & INDEX_TO_POS_MASK;
    }

    public static int sectionToCube(int val) {
        return val >> (LOG2_BLOCK_SIZE - 4);
    }

    public static int sectionToIndex(int sectionX, int sectionY, int sectionZ) {
        return blockToIndex(sectionX << 4, sectionY << 4, sectionZ << 4);
    }

    public static int cubeToSection(int cube, int section) {
        return cube << (LOG2_BLOCK_SIZE - 4) | section;
    }

    public static int sectionToCubeCeil(int viewDistance) {
        return MathUtil.ceilDiv(viewDistance, BLOCK_SIZE_DIV_16);
    }

    public static int sectionToCubeRenderDistance(int viewDistance) {
        return Math.max(3, sectionToCubeCeil(viewDistance));
    }

    public static int blockToSection(int block)
    {
        return block >> 4;
    }

    public static int sectonToMinBlock(int section) {
        return section << 4;
    }
}