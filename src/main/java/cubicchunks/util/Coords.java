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
package cubicchunks.util;

import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Coords {

    public static final int NO_HEIGHT = Integer.MIN_VALUE + 32;

    public static BlockPos midPos(BlockPos p1, BlockPos p2) {
        //bitshift instead of / - round always down
        return new BlockPos((p1.getX() + p2.getX()) >> 1, (p1.getY() + p2.getY()) >> 1, (p1.getZ() + p2.getZ()) >> 1);
    }

    public static int blockToLocal(int val) {
        return val & 0xf;
    }

    public static int blockToCube(int val) {
        return val >> 4;
    }

    public static int blockCeilToCube(int val) {
        return -((-val) >> 4);
    }

    public static int localToBlock(int cubeVal, int localVal) {
        return cubeToMinBlock(cubeVal) + localVal;
    }

    public static int cubeToMinBlock(int val) {
        return val << 4;
    }

    public static int cubeToMaxBlock(int val) {
        return cubeToMinBlock(val) + 15;
    }

    public static int getCubeXForEntity(Entity entity) {
        return blockToCube(MathHelper.floor(entity.posX));
    }

    public static int getCubeZForEntity(Entity entity) {
        return blockToCube(MathHelper.floor(entity.posZ));
    }

    public static int getCubeYForEntity(Entity entity) {
        // the entity is in the cube it's inside, not the cube it's standing on
        return blockToCube(MathHelper.floor(entity.posY));
    }

    public static BlockPos getCubeCenter(Cube cube) {
        return new BlockPos(
                cubeToMinBlock(cube.getX()) + 8,
                cubeToMinBlock(cube.getY()) + 8,
                cubeToMinBlock(cube.getZ()) + 8
        );
    }

    public static int blockToCube(double blockPos) {
        return blockToCube(MathHelper.floor(blockPos));
    }

    public static int cubeToCenterBlock(int cubeVal) {
        return localToBlock(cubeVal, Cube.SIZE / 2);
    }
}
