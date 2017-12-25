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

import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.ServerHeightMap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ILightBlockAccess {

    int getBlockLightOpacity(BlockPos pos);

    int getLightFor(EnumSkyBlock lightType, BlockPos pos);

    void setLightFor(EnumSkyBlock lightType, BlockPos pos, int val);

    default boolean canSeeSky(BlockPos pos) {
        return pos.getY() > getTopBlockY(pos);
    }

    int getEmittedLight(BlockPos pos, EnumSkyBlock type);

    default int getLightFromNeighbors(EnumSkyBlock type, BlockPos pos) {
        return Math.max(0, getUnclampedLightFromNeighbors(type, pos));
    }

    default int getUnclampedLightFromNeighbors(EnumSkyBlock type, BlockPos pos) {
        //TODO: use MutableBlockPos?
        int max = 0;
        for (EnumFacing direction : EnumFacing.values()) {
            int light = getLightFor(type, pos.offset(direction));
            if (light > max) {
                max = light;
            }
        }
        int decrease = Math.max(1, getBlockLightOpacity(pos));
        return max - decrease;
    }

    /**
     * Returns Y coordinate of the top already committed opaque block (block Y coordinate ignored)
     */
    int getTopBlockY(BlockPos pos);

    /**
     * Returns Y coordinate of the top opaque block that is already committed or uncommitted but in the specified cube (block Y coordinate ignored)
     */
    default int getEffectiveTopBlockY(CubePos cube, BlockPos pos) {
        return Math.max(getLocalTopBlockY(cube, pos), getTopBlockY(pos));
    }

    /**
     * Returns Y coordinate of the top opaque block in given cube, at given coordinates (block Y coordinate ignored), or block right below that
     * cube if there is none.
     */
    default int getLocalTopBlockY(CubePos cube, BlockPos pos) {
        int bottomY = cube.getMinBlockY();
        for (int y = cube.getMaxBlockY(); y >= bottomY; y--) {
            if (getBlockLightOpacity(new BlockPos(pos.getX(), y, pos.getZ())) != 0) {
                return y;
            }
        }
        return bottomY - 1;
    }
}
