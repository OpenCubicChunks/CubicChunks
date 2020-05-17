package cubicchunks.cc.chunk.graph;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.lighting.LevelBasedGraph;

public abstract class CubicChunkDistanceGraph  extends LevelBasedGraph {
    protected CubicChunkDistanceGraph(int x, int y, int z) {
        super(x, y, z);
    }

    protected boolean isRoot(long pos) {
        return pos == CubeChunkPos.SENTINEL;
    }

    protected void notifyNeighbors(long pos, int level, boolean isDecreasing) {
        SectionPos cubePos =  SectionPos.from(pos);
        int x = cubePos.getX();
        int y = cubePos.getY();
        int z = cubePos.getZ();

        for(int x2 = -1; x2 <= 1; ++x2) {
            for (int y2 = -1; y2 <= 1; ++y2) {
                for (int z2 = -1; z2 <= 1; ++z2) {
                    long i1 = SectionPos.asLong(x + x2, y + y2, z + z2);
                    if (i1 != pos) {
                        this.propagateLevel(pos, i1, level, isDecreasing);
                    }
                }
            }
        }

    }

    /**
     * Computes level propagated from neighbors of specified position with given existing level, excluding the given
     * source position.
     */
    protected int computeLevel(long pos, long excludedSourcePos, int level) {
        int i = level;
        SectionPos cubePos =  SectionPos.from(pos);
        int x = cubePos.getX();
        int y = cubePos.getY();
        int z = cubePos.getZ();

        for(int x2 = -1; x2 <= 1; ++x2) {
            for (int y2 = -1; y2 <= 1; ++y2) {
                for (int z2 = -1; z2 <= 1; ++z2) {
                    long j1 = SectionPos.asLong(x + x2,y + y2 , z + z2);
                    if (j1 == pos) {
                        j1 = CubeChunkPos.SENTINEL;
                    }

                    if (j1 != excludedSourcePos) {
                        int k1 = this.getEdgeLevel(j1, pos, this.getLevel(j1));
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
    protected int getEdgeLevel(long startPos, long endPos, int startLevel) {
        return startPos == CubeChunkPos.SENTINEL ? this.getSourceLevel(endPos) : startLevel + 1;
    }

    protected abstract int getSourceLevel(long pos);

    public void updateSourceLevel(long pos, int level, boolean isDecreasing) {
        this.scheduleUpdate(CubeChunkPos.SENTINEL, pos, level, isDecreasing);
    }

}