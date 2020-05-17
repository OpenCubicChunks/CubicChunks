package cubicchunks.cc.chunk.graph;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

//Essentially take the ChunkPos and add a 3rd Dimension too it but the goal is too do everything out of SectionPos.
public class CubeChunkPos  {
    /**
     * Value representing an absent or invalid cubechunkpos
     */
    public static final long SENTINEL = asLong(1875016, 1875016, 1875016);
    public final int cubeX;
    public final int cubeZ;
    public final int cubeY;

    public CubeChunkPos(int cubeX, int cubeY, int cubeZ) {
        this.cubeX = cubeX;
        this.cubeY = cubeY;
        this.cubeZ = cubeZ;
    }

    public CubeChunkPos(BlockPos pos) {
        this.cubeX = pos.getX() >> 4;
        this.cubeY = pos.getY() >> 4;
        this.cubeZ = pos.getZ() >> 4;
    }

    public CubeChunkPos(long longIn) {
        this.cubeX = (int) longIn;
        this.cubeY = (int) longIn;
        this.cubeZ = (int) (longIn >> 32);
    }

    /**
     * Converts the chunk coordinate pair to a long
     */
    public static long asLong(int x, int y, int z) {
        return (long) x & 4294967295L | ((long) y & 4294967295L) | ((long) z & 4294967295L) << 32;
    }

    public static int getX(long chunkAsLong) {
        return (int) (chunkAsLong & 4294967295L);
    }

    public static int getY(long chunkAsLong) {
        return (int) (chunkAsLong & 4294967295L);
    }

    public static int getZ(long chunkAsLong) {
        return (int) (chunkAsLong >>> 32 & 4294967295L);
    }

    public static Stream<CubeChunkPos> getAllInBox(CubeChunkPos center, int radius) {
        return getAllInBox(new CubeChunkPos(center.cubeX - radius, center.cubeY - radius, center.cubeZ - radius), new CubeChunkPos(center.cubeX + radius, center.cubeY - radius, center.cubeZ + radius));
    }

    public static Stream<CubeChunkPos> getAllInBox(final CubeChunkPos start, final CubeChunkPos end) {
        int x = Math.abs(start.cubeX - end.cubeX) + 1;
        int y = Math.abs(start.cubeY - end.cubeY) + 1;
        int z = Math.abs(start.cubeZ - end.cubeZ) + 1;
        final int startEndX = start.cubeX < end.cubeX ? 1 : -1;
        final int startEndY = start.cubeY < end.cubeY ? 1 : -1;
        final int startEndZ = start.cubeZ < end.cubeZ ? 1 : -1;
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<CubeChunkPos>(x * z, 64) {
            @Nullable
            private CubeChunkPos current;

            public boolean tryAdvance(Consumer<? super CubeChunkPos> p_tryAdvance_1_) {
                if (this.current == null) {
                    this.current = start;
                } else {
                    int currentX = this.current.cubeX;
                    int currentY = this.current.cubeY;
                    int currentZ = this.current.cubeZ;
                    if (currentX == end.cubeX) {
                        if (currentZ == end.cubeZ) {
                            return false;
                        }

                        this.current = new CubeChunkPos(start.cubeX, currentY, currentZ + startEndZ);
                    } else {
                        this.current = new CubeChunkPos(currentX + startEndX, currentY, currentZ);
                    }
                }

                p_tryAdvance_1_.accept(this.current);
                return true;
            }
        }, false);
    }

    public long asLong() {
        return asLong(this.cubeX, this.cubeY, this.cubeZ);
    }

    public int hashCode() {
        int i = 1664525 * this.cubeX + 1013904223;
        int j = 1664525 * (this.cubeZ ^ -559038737) + 1013904223;
        return i ^ j;
    }

    public boolean equals(Object cubeChunkPos) {
        if (this == cubeChunkPos) {
            return true;
        } else if (!(cubeChunkPos instanceof CubeChunkPos)) {
            return false;
        } else {
            CubeChunkPos chunkpos = (CubeChunkPos) cubeChunkPos;
            return this.cubeX == chunkpos.cubeX && this.cubeY == chunkpos.cubeY && this.cubeZ == chunkpos.cubeZ;
        }
    }

    /**
     * Get the first world X coordinate that belongs to this Chunk
     */
    public int getXStart() {
        return this.cubeX << 4;
    }

    /**
     * Get the first world Y coordinate that belongs to this Chunk
     */
    public int getYStart() {
        return this.cubeY << 4;
    }

    /**
     * Get the first world Z coordinate that belongs to this Chunk
     */
    public int getZStart() {
        return this.cubeZ << 4;
    }

    /**
     * Get the last world X coordinate that belongs to this Chunk
     */
    public int getXEnd() {
        return (this.cubeX << 4) + 15;
    }

    /**
     * Get the last world Y coordinate that belongs to this Chunk
     */
    public int getYEnd() {
        return (this.cubeY << 4) + 15;
    }

    /**
     * Get the last world Z coordinate that belongs to this Chunk
     */
    public int getZEnd() {
        return (this.cubeZ << 4) + 15;
    }

    /**
     * Gets the x-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordX() {
        return this.cubeX >> 5;
    }

    /**
     * Gets the y-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordY() {
        return this.cubeY >> 5;
    }

    /**
     * Gets the z-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordZ() {
        return this.cubeZ >> 5;
    }

    /**
     * Gets the x-coordinate of this chunk within the region file that contains it.
     */
    public int getRegionPositionX() {
        return this.cubeX & 31;
    }

    /**
     * Gets the z-coordinate of this chunk within the region file that contains it.
     */
    public int getRegionPositionZ() {
        return this.cubeZ & 31;
    }

    /**
     * Get the World coordinates of the Block with the given Chunk coordinates relative to this chunk
     */
    public BlockPos getBlock(int x, int y, int z) {
        return new BlockPos((this.cubeX << 4) + x, (this.cubeY << 4) + 4, (this.cubeZ << 4) + z);
    }

    public String toString() {
        return "[" + this.cubeX + ", " + this.cubeY + ", " + this.cubeZ + "]";
    }

    public BlockPos getBlock() {
        return new BlockPos(this.cubeX << 4, this.cubeY << 4, this.cubeZ << 4);
    }

    public int getChessboardDistance(CubeChunkPos chunkPosIn) {
        return Math.max(Math.abs(this.cubeX - chunkPosIn.cubeX), Math.abs(this.cubeZ - chunkPosIn.cubeZ));
    }
}
