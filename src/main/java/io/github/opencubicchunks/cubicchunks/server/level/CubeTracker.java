package io.github.opencubicchunks.cubicchunks.chunk.graph;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class CubeDistanceGraph extends DynamicGraphMinFixedPoint {

    private final int xR;
    private final int yR;
    private final int zR;

    protected CubeDistanceGraph(int levelCount, int expectedUpdatesByLevel, int expectedPropagationLevels, int xR, int yR, int zR) {
        super(levelCount, expectedUpdatesByLevel, expectedPropagationLevels);
        this.xR = xR;
        this.yR = yR;
        this.zR = zR;
    }

    protected CubeDistanceGraph(int levelCount, int expectedUpdatesByLevel, int expectedPropagationLevels) {
        this(levelCount, expectedUpdatesByLevel, expectedPropagationLevels, 1, 1, 1);
    }

    @Override protected boolean isSource(long pos) {
        return pos == Long.MAX_VALUE;
    }

    @Override protected void checkNeighborsAfterUpdate(long pos, int level, boolean isDecreasing) {
        CubePos cubePos = CubePos.from(pos);
        int x = cubePos.getX();
        int y = cubePos.getY();
        int z = cubePos.getZ();

        for (int x2 = -xR; x2 <= xR; ++x2) {
            for (int y2 = -yR; y2 <= yR; ++y2) {
                for (int z2 = -zR; z2 <= zR; ++z2) {
                    long cubeLong = CubePos.asLong(x + x2, y + y2, z + z2);
                    if (cubeLong != pos) {
                        this.checkNeighbor(pos, cubeLong, level, isDecreasing);
                    }
                }
            }
        }
    }

    /**
     * Computes level propagated from neighbors of specified position with given existing level, excluding the given source position.
     */
    @Override protected int getComputedLevel(long pos, long excludedSourcePos, int level) {
        int i = level;
        CubePos cubePos = CubePos.from(pos);
        int x = cubePos.getX();
        int y = cubePos.getY();
        int z = cubePos.getZ();

        for (int x2 = -xR; x2 <= xR; ++x2) {
            for (int y2 = -yR; y2 <= yR; ++y2) {
                for (int z2 = -zR; z2 <= zR; ++z2) {
                    long cubeLong = CubePos.asLong(x + x2, y + y2, z + z2);
                    if (cubeLong == pos) {
                        cubeLong = Long.MAX_VALUE;
                    }

                    if (cubeLong != excludedSourcePos) {
                        int k1 = this.computeLevelFromNeighbor(cubeLong, pos, this.getLevel(cubeLong));
                        if (i > k1) {
                            i = k1;
                        }

                        if (i == 0) {
                            return i;
                        }
                    }
                }
            }
        }

        return i;
    }

    /**
     * Returns level propagated from start position with specified level to the neighboring end position.
     */
    @Override protected int computeLevelFromNeighbor(long startPos, long endPos, int startLevel) {
        return startPos == Long.MAX_VALUE ? this.getSourceLevel(endPos) : startLevel + 1;
    }

    protected abstract int getSourceLevel(long pos);

    public void updateSourceLevel(long pos, int level, boolean isDecreasing) {
        this.checkEdge(Long.MAX_VALUE, pos, level, isDecreasing);
    }

}