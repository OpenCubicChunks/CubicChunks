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

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.Queues;

import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IntegerCache;
import net.minecraft.util.math.BlockPos;
import static cubicchunks.client.RenderConstants.*;

public class VisGraphBigChunkAdapted extends VisGraph {

    private static final int DX = (int) Math.pow(RENDER_CHUNK_SIZE, 0.0D);
    private static final int DZ = (int) Math.pow(RENDER_CHUNK_SIZE, 1.0D);
    private static final int DY = (int) Math.pow(RENDER_CHUNK_SIZE, 2.0D);
    private final BitSet bitSet = new BitSet(RENDER_CHUNK_BLOCKS_AMOUNT);
    private static final int[] INDEX_OF_EDGES = new int[RENDER_CHUNK_EDGE_BLOCK_AMOUNT];
    private int empty = RENDER_CHUNK_BLOCKS_AMOUNT;

    public void setOpaqueCube(BlockPos pos) {
        this.bitSet.set(getIndex(pos), true);
        --this.empty;
    }

    public void setOpaqueCube(int localX, int localY, int localZ) {
        this.bitSet.set(getIndex(localX, localY, localZ), true);
        --this.empty;
    }

    private static int getIndex(BlockPos pos) {
        return getIndex(pos.getX() & RENDER_CHUNK_MAX_POS, pos.getY() & RENDER_CHUNK_MAX_POS, pos.getZ() & RENDER_CHUNK_MAX_POS);
    }

    private static int getIndex(int x, int y, int z) {
        return x << 0 | y << RENDER_CHUNK_SIZE_BIT * 2 | z << RENDER_CHUNK_SIZE_BIT;
    }

    public SetVisibility computeVisibility() {
        SetVisibility setvisibility = new SetVisibility();

        if (RENDER_CHUNK_BLOCKS_AMOUNT - this.empty < RENDER_CHUNK_BLOCKS_AMOUNT >> 4) {
            setvisibility.setAllVisible(true);
        } else if (this.empty == 0) {
            setvisibility.setAllVisible(false);
        } else {
            for (int i : INDEX_OF_EDGES) {
                if (!this.bitSet.get(i)) {
                    setvisibility.setManyVisible(this.floodFill(i));
                }
            }
        }

        return setvisibility;
    }

    public Set<EnumFacing> getVisibleFacings(BlockPos pos) {
        return this.floodFill(getIndex(pos));
    }

    private Set<EnumFacing> floodFill(int pos) {
        Set<EnumFacing> set = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
        Queue<Integer> queue = Queues.<Integer>newArrayDeque();
        queue.add(IntegerCache.getInteger(pos));
        this.bitSet.set(pos, true);

        while (!queue.isEmpty()) {
            int i = ((Integer) queue.poll()).intValue();
            this.addEdges(i, set);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                int j = this.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !this.bitSet.get(j)) {
                    this.bitSet.set(j, true);
                    queue.add(IntegerCache.getInteger(j));
                }
            }
        }

        return set;
    }

    private void addEdges(int pos, Set<EnumFacing> facingSet) {
        int i = pos >> 0 & RENDER_CHUNK_MAX_POS;

        if (i == 0) {
            facingSet.add(EnumFacing.WEST);
        } else if (i == RENDER_CHUNK_MAX_POS) {
            facingSet.add(EnumFacing.EAST);
        }

        int j = pos >> RENDER_CHUNK_SIZE_BIT * 2 & RENDER_CHUNK_MAX_POS;

        if (j == 0) {
            facingSet.add(EnumFacing.DOWN);
        } else if (j == RENDER_CHUNK_MAX_POS) {
            facingSet.add(EnumFacing.UP);
        }

        int k = pos >> RENDER_CHUNK_SIZE_BIT & RENDER_CHUNK_MAX_POS;

        if (k == 0) {
            facingSet.add(EnumFacing.NORTH);
        } else if (k == RENDER_CHUNK_MAX_POS) {
            facingSet.add(EnumFacing.SOUTH);
        }
    }

    private int getNeighborIndexAtFace(int pos, EnumFacing facing) {
        switch (facing) {
            case DOWN:

                if ((pos >> RENDER_CHUNK_SIZE_BIT * 2 & RENDER_CHUNK_MAX_POS) == 0) {
                    return -1;
                }

                return pos - DY;
            case UP:

                if ((pos >> RENDER_CHUNK_SIZE_BIT * 2 & RENDER_CHUNK_MAX_POS) == RENDER_CHUNK_MAX_POS) {
                    return -1;
                }

                return pos + DY;
            case NORTH:

                if ((pos >> RENDER_CHUNK_SIZE_BIT & RENDER_CHUNK_MAX_POS) == 0) {
                    return -1;
                }

                return pos - DZ;
            case SOUTH:

                if ((pos >> RENDER_CHUNK_SIZE_BIT & RENDER_CHUNK_MAX_POS) == RENDER_CHUNK_MAX_POS) {
                    return -1;
                }

                return pos + DZ;
            case WEST:

                if ((pos >> 0 & RENDER_CHUNK_MAX_POS) == 0) {
                    return -1;
                }

                return pos - DX;
            case EAST:

                if ((pos >> 0 & RENDER_CHUNK_MAX_POS) == RENDER_CHUNK_MAX_POS) {
                    return -1;
                }

                return pos + DX;
            default:
                return -1;
        }
    }

    static {
        int k = 0;
        for (int l = 0; l < RENDER_CHUNK_SIZE; ++l) {
            for (int i1 = 0; i1 < RENDER_CHUNK_SIZE; ++i1) {
                for (int j1 = 0; j1 < RENDER_CHUNK_SIZE; ++j1) {
                    if (l == 0 || l == RENDER_CHUNK_MAX_POS || i1 == 0 || i1 == RENDER_CHUNK_MAX_POS || j1 == 0 || j1 == RENDER_CHUNK_MAX_POS) {
                        INDEX_OF_EDGES[k++] = getIndex(l, i1, j1);
                    }
                }
            }
        }
    }
}
