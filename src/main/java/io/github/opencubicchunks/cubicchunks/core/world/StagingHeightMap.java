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
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

public class StagingHeightMap implements IHeightMap {

    private final List<ICube> stagedCubes = new ArrayList<>();
    private final int[] heightmap = new int[ICube.SIZE * ICube.SIZE];
    private final BitSet dirtyFlag = new BitSet(heightmap.length);

    public void addStagedCube(ICube cube) {
        stagedCubes.add(cube);
        // top-to-bottom order
        stagedCubes.sort(Comparator.comparingInt(c -> -c.getCoords().getY()));
        // TODO: optimize
        if (!cube.isEmpty()) {
            dirtyFlag.set(0, heightmap.length);
        }
    }

    public void removeStagedCube(ICube cube) {
        if (stagedCubes.remove(cube)) {
            // TODO: optimize
            if (!cube.isEmpty()) {
                dirtyFlag.set(0, heightmap.length);
            }
        }
    }

    @Override public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        if (opacity > 0) {
            if (blockY > getTopBlockY(localX, localZ)) {
                heightmap[index(localX, localZ)] = blockY;
            }
        } else if(blockY == getTopBlockY(localX, localZ)) {
            dirtyFlag.set(index(localX, localZ));
        }
    }

    private int index(int localX, int localZ) {
        return (localZ << 4) | localX;
    }

    @Override public int getTopBlockY(int localX, int localZ) {
        int idx = index(localX, localZ);
        if (!dirtyFlag.get(idx)) {
            return heightmap[idx];
        }
        dirtyFlag.clear(idx);
        return heightmap[idx] = computeHeightMap(localX, localZ);
    }

    private int computeHeightMap(int localX, int localZ) {
        for (int j = 0, stagedCubesSize = stagedCubes.size(); j < stagedCubesSize; j++) {
            ICube stagedCube = stagedCubes.get(j);
            ExtendedBlockStorage ebs = stagedCube.getStorage();
            if (ebs == null || ebs.isEmpty()) {
                continue;
            }
            for (int i = 15; i >= 0; i--) {
                if (ebs.get(localX, i, localZ).getLightOpacity() > 0) {
                    return Coords.localToBlock(stagedCube.getY(), i);
                }
            }
        }
        return Coords.NO_HEIGHT;
    }

    @Override public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        throw new UnsupportedOperationException("Not implemented for staging heightmap");
    }

    @Override public int getLowestTopBlockY() {
        throw new UnsupportedOperationException("Not implemented for staging heightmap");
    }
}
