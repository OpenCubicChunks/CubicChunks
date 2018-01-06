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
package cubicchunks.client;

import java.util.Set;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class RenderVariables {

    private static int renderChunkSizeBit;
    private static int renderChunkSizeBitShiftChunkPos;
    private static int renderChunkSize;
    private static int renderChunkBlocksAmount;
    private static int renderChunkEdgeBlockAmount;
    private static int renderChunkMaxPos;
    private static int renderChunkStartPosMask;
    private static double renderChunkCenterPos;
    private static int[] visGraphIndexOfEdges;
    private static int visGraphDX;
    private static int visGraphDZ;
    private static int visGraphDY;

    static {
        setRenderChunkBit(5);
    }

    public static void setRenderChunkBit(int bit) {
        if (bit < 4 || bit > 8)
            throw new IllegalArgumentException("Size of a RenderChunk should be between 16 and 256 blocks.");
        renderChunkSizeBit = bit;
        renderChunkSizeBitShiftChunkPos = renderChunkSizeBit - 4;
        renderChunkSize = 1 << renderChunkSizeBit;
        renderChunkBlocksAmount = renderChunkSize * renderChunkSize * renderChunkSize;
        renderChunkEdgeBlockAmount =
                renderChunkBlocksAmount - (renderChunkSize - 2) * (renderChunkSize - 2) * (renderChunkSize - 2);
        renderChunkMaxPos = renderChunkSize - 1;
        renderChunkStartPosMask = 0xFFFFFFFF ^ renderChunkMaxPos;
        renderChunkCenterPos = renderChunkSize / 2.0D;
        visGraphDX = 1;
        visGraphDZ = renderChunkSize;
        visGraphDY = renderChunkSize * renderChunkSize;
        visGraphIndexOfEdges = new int[renderChunkEdgeBlockAmount];
        int index = 0;
        for (int x = 0; x < renderChunkSize; ++x) {
            for (int y = 0; y < renderChunkSize; ++y) {
                for (int z = 0; z < renderChunkSize; ++z) {
                    if (x == 0 || x == renderChunkMaxPos || y == 0 || y == renderChunkMaxPos || z == 0 || z == renderChunkMaxPos) {
                        visGraphIndexOfEdges[index++] = getIndex(x, y, z);
                    }
                }
            }
        }
    }

    public static int getRenderChunkBit() {
        return renderChunkSizeBit;
    }

    public static int getRenderChunkPosShitBit() {
        return renderChunkSizeBitShiftChunkPos;
    }

    public static int getRenderChunkBlocksAmount() {
        return renderChunkBlocksAmount;
    }

    public static int getRenderChunkSize() {
        return renderChunkSize;
    }

    public static int getRenderStartPosMask() {
        return renderChunkStartPosMask;
    }

    public static int getRenderChunkMaxPos() {
        return renderChunkMaxPos;
    }

    public static double getRenderChunkCenterPos() {
        return renderChunkCenterPos;
    }

    public static int getIndex(int x, int y, int z) {
        return x << 0 | y << renderChunkSizeBit * 2 | z << renderChunkSizeBit;
    }

    public static int getIndex(BlockPos pos) {
        return getIndex(pos.getX() & renderChunkMaxPos, pos.getY() & renderChunkMaxPos, pos.getZ() & renderChunkMaxPos);
    }

    public static int[] getVisGraphIndexOfEdges() {
        return visGraphIndexOfEdges;
    }

    public static void addEdges(int pos, Set<EnumFacing> facingSet) {
        int i = pos >> 0 & renderChunkMaxPos;

        if (i == 0) {
            facingSet.add(EnumFacing.WEST);
        } else if (i == renderChunkMaxPos) {
            facingSet.add(EnumFacing.EAST);
        }

        int j = pos >> renderChunkSizeBit * 2 & renderChunkMaxPos;

        if (j == 0) {
            facingSet.add(EnumFacing.DOWN);
        } else if (j == renderChunkMaxPos) {
            facingSet.add(EnumFacing.UP);
        }

        int k = pos >> renderChunkSizeBit & renderChunkMaxPos;

        if (k == 0) {
            facingSet.add(EnumFacing.NORTH);
        } else if (k == renderChunkMaxPos) {
            facingSet.add(EnumFacing.SOUTH);
        }
    }

    public static int getNeighborIndexAtFace(int pos, EnumFacing facing) {
        int x = pos & renderChunkMaxPos;
        int y = pos >> renderChunkSizeBit * 2 & renderChunkMaxPos;
        int z = pos >> renderChunkSizeBit & renderChunkMaxPos;
        switch (facing) {
            case DOWN:
                if (y == 0)
                    return -1;
                return pos - visGraphDY;
            case UP:
                if (y == renderChunkMaxPos)
                    return -1;
                return pos + visGraphDY;
            case NORTH:
                if (z == 0)
                    return -1;
                return pos - visGraphDZ;
            case SOUTH:
                if (z == renderChunkMaxPos)
                    return -1;
                return pos + visGraphDZ;
            case WEST:
                if (x == 0)
                    return -1;
                return pos - visGraphDX;
            case EAST:
                if (x == renderChunkMaxPos)
                    return -1;
                return pos + visGraphDX;
            default:
                return -1;
        }
    }
}
