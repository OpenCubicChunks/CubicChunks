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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMaxBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToMinBlock;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.localToBlock;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class SkyLightUpdateCubeSelector {

    private SkyLightUpdateCubeSelector() {
        throw new RuntimeException();
    }

    /**
     * Returns Set of cube Y locations that can be updated for give light update.
     *
     * @param column Column to select cubes from
     * @param localX in-column X position
     * @param localZ in-column Z position
     * @param minBlockY minimum light update Y. Integer.MIN_VALUE for no lower limit.
     * @param maxBlockY position from which updating should be started. Integer.MAX_VALUE tu update from top of the
     * world.
     *
     * @return set of affected cube Y positions
     */
    static TIntSet getCubesY(Chunk column, int localX, int localZ, int minBlockY, int maxBlockY) {
        // NOTE: maxBlockY is always the air block above the top block that was added or removed
        World world = column.getWorld();

        TIntSet cubesToDiffuse = new TIntHashSet();

        if (!world.provider.hasSkyLight()) {
            return cubesToDiffuse;
        }

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(
                localToBlock(column.x, localX),
                maxBlockY - 1,
                localToBlock(column.z, localZ)
        );

        int newMaxBlockY = column.getHeightValue(localX, localZ) - 1;

        //if there is no min block - there are no blocks here
        //so assume it's at Integer.MIN_VALUE
        int maxCubeY = Coords.blockToCube(newMaxBlockY);

        //attempt to update lighting only in loaded cubes
        for (ICube cube : ((IColumn) column).getLoadedCubes()) {
            int cubeY = cube.getY();
            int minCubeBlockY = cubeY * Cube.SIZE;

            //do we even need to do anything here?
            if (maxBlockY < minCubeBlockY) {
                continue;
            }
            if (cubeY > maxCubeY) {
                //if light value at the bottom is already correct - nothing to do here
                //so update only if incorrect
                blockPos.setPos(localToBlock(cube.getX(), localX), cubeToMinBlock(cubeY), localToBlock(cube.getZ(), localZ));
                if (cube.getLightFor(EnumSkyBlock.SKY, blockPos) != 15) {
                    cubesToDiffuse.add(cube.getY());
                }
            } else if (cubeY == maxCubeY) {
                //current top block is the actual reason to update
                //so cube that contains it needs update
                cubesToDiffuse.add(cube.getY());
                //light can propagate to cube below too
                if ((cube = ((IColumn) column).getLoadedCube(maxCubeY - 1)) != null) {
                    cubesToDiffuse.add(cube.getY());
                }
            } else if (cubeY == maxCubeY - 1) {
                //it's handled by cubeY == maxCubeY case
                continue;
            } else {
                assert cubeY < maxCubeY - 1;
                blockPos.setPos(localToBlock(cube.getX(), localX), cubeToMaxBlock(cubeY), localToBlock(cube.getZ(), localZ));
                //if we are below minBlockY or if the top block has correct light value (0) - nothing to do
                if (minCubeBlockY + 15 < minBlockY || cube.getLightFor(EnumSkyBlock.SKY, blockPos) == 0) {
                    continue;
                }
                cubesToDiffuse.add(cube.getY());
            }
        }

        return cubesToDiffuse;
    }
}
