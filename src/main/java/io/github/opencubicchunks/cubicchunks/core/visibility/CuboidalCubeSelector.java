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
package io.github.opencubicchunks.cubicchunks.core.visibility;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CuboidalCubeSelector extends CubeSelector {

    @Override
    public void forAllVisibleFrom(CubePos cubePos, int horizontalViewDistance, int verticalViewDistance, Consumer<CubePos> consumer) {
        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();
        for (int x = cubeX - horizontalViewDistance; x <= cubeX + horizontalViewDistance; x++) {
            for (int y = cubeY - verticalViewDistance; y <= cubeY + verticalViewDistance; y++) {
                for (int z = cubeZ - horizontalViewDistance; z <= cubeZ + horizontalViewDistance; z++) {
                    consumer.accept(new CubePos(x, y, z));
                }
            }
        }
    }

    @Override
    public void findChanged(CubePos oldPos, CubePos newPos,
            int horizontalViewDistance, int verticalViewDistance,
            Set<CubePos> cubesToRemove, Set<CubePos> cubesToLoad,
            Set<ChunkPos> columnsToRemove, Set<ChunkPos> columnsToLoad) {
        int oldX = oldPos.getX();
        int oldY = oldPos.getY();
        int oldZ = oldPos.getZ();
        int newX = newPos.getX();
        int newY = newPos.getY();
        int newZ = newPos.getZ();
        int dx = newX - oldX;
        int dy = newY - oldY;
        int dz = newZ - oldZ;

        for (int currentX = newX - horizontalViewDistance; currentX <= newX + horizontalViewDistance; ++currentX) {
            for (int currentZ = newZ - horizontalViewDistance; currentZ <= newZ + horizontalViewDistance; ++currentZ) {
                //first handle columns
                //is current position outside of the old render distance square?
                if (!this.isPointWithinCubeVolume(oldX, 0, oldZ, currentX, 0, currentZ, horizontalViewDistance, verticalViewDistance)) {
                    columnsToLoad.add(new ChunkPos(currentX, currentZ));
                }

                //if we moved the current point to where it would be previously,
                //would it be outside of current render distance square?
                if (!this.isPointWithinCubeVolume(newX, 0, newZ, currentX - dx, 0, currentZ - dz, horizontalViewDistance, verticalViewDistance)) {
                    columnsToRemove.add(new ChunkPos(currentX - dx, currentZ - dz));
                }
                for (int currentY = newY - verticalViewDistance; currentY <= newY + verticalViewDistance; ++currentY) {
                    //now handle cubes, the same way
                    if (!this.isPointWithinCubeVolume(oldX, oldY, oldZ, currentX, currentY, currentZ,
                            horizontalViewDistance, verticalViewDistance)) {
                        cubesToLoad.add(new CubePos(currentX, currentY, currentZ));
                    }
                    if (!this.isPointWithinCubeVolume(newX, newY, newZ, currentX - dx, currentY - dy, currentZ - dz,
                            horizontalViewDistance, verticalViewDistance)) {
                        cubesToRemove.add(new CubePos(currentX - dx, currentY - dy, currentZ - dz));
                    }
                }
            }
        }

        assert cubesToLoad.stream().allMatch(pos -> !cubesToRemove.contains(pos)) : "cubesToRemove contains element from cubesToLoad!";
        assert columnsToLoad.stream().allMatch(pos -> !columnsToRemove.contains(pos)) : "columnsToRemove contains element from columnsToLoad!";
    }

    @Override
    public void findAllUnloadedOnViewDistanceDecrease(CubePos playerPos,
            int oldHorizontalViewDistance, int newHorizontalViewDistance,
            int oldVerticalViewDistance, int newVerticalViewDistance,
            Set<CubePos> cubesToUnload, Set<ChunkPos> columnsToUnload) {
        int playerCubeX = playerPos.getX();
        int playerCubeY = playerPos.getY();
        int playerCubeZ = playerPos.getZ();

        for (int cubeX = playerCubeX - oldHorizontalViewDistance; cubeX <= playerCubeX + oldHorizontalViewDistance; cubeX++) {
            for (int cubeZ = playerCubeZ - oldHorizontalViewDistance; cubeZ <= playerCubeZ + oldHorizontalViewDistance; cubeZ++) {
                if (!isPointWithinCubeVolume(playerCubeX, 0, playerCubeZ, cubeX, 0, cubeZ, newHorizontalViewDistance, newVerticalViewDistance)) {
                    columnsToUnload.add(new ChunkPos(cubeX, cubeZ));
                }
                for (int cubeY = playerCubeY - oldVerticalViewDistance; cubeY <= playerCubeY + oldVerticalViewDistance; cubeY++) {
                    if (!isPointWithinCubeVolume(playerCubeX, playerCubeY, playerCubeZ, cubeX, cubeY, cubeZ, newHorizontalViewDistance,
                            newVerticalViewDistance)) {
                        cubesToUnload.add(new CubePos(cubeX, cubeY, cubeZ));
                    }
                }
            }
        }
    }

    private boolean isPointWithinCubeVolume(int cubeX, int cubeY, int cubeZ, int pointX, int pointY, int pointZ, int horizontal, int vertical) {
        int dx = cubeX - pointX;
        int dy = cubeY - pointY;
        int dz = cubeZ - pointZ;
        return dx >= -horizontal && dx <= horizontal
                && dy >= -vertical && dy <= vertical
                && dz >= -horizontal && dz <= horizontal;
    }
}
