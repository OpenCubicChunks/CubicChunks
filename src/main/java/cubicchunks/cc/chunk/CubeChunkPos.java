package cubicchunks.cc.chunk;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CubeChunkPos {
    /** Value representing an absent or invalid chunkpos */
    public static final long SENTINEL = asLong(1875016, 1875016, 1875016);
    public final int x;
    public  int y;
    public final int z;

    public CubeChunkPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CubeChunkPos(BlockPos pos) {
        this.x = pos.getX() >> 4;
        this.y = pos.getY() >> 4;
        this.z = pos.getZ() >> 4;
    }

    public CubeChunkPos(long longIn) {
        this.x = (int)longIn;
        this.y = (int)longIn;
        this.z = (int)(longIn >> 32);
    }

    public long asLong() {
        return asLong(this.x, this.y, this.z);
    }

    /**
     * Converts the chunk coordinate pair to a long
     */
    public static long asLong(int x, int y, int z) {
        return (long)x & 4294967295L | ((long)y & 4294967295L) | ((long)z & 4294967295L) << 32;
    }

    public static int getX(long chunkAsLong) {
        return (int)(chunkAsLong & 4294967295L);
    }

    public static int getY(long chunkAsLong) {
        return (int)(chunkAsLong & 4294967295L);
    }

    public static int getZ(long chunkAsLong) {
        return (int)(chunkAsLong >>> 32 & 4294967295L);
    }

    public int hashCode() {
        int i = 1664525 * this.x + 1013904223;
        int j = 1664525 * (this.z ^ -559038737) + 1013904223;
        return i ^ j;
    }

    public boolean equals(Object cubeChunkPos) {
        if (this == cubeChunkPos) {
            return true;
        } else if (!(cubeChunkPos instanceof CubeChunkPos)) {
            return false;
        } else {
            CubeChunkPos chunkpos = (CubeChunkPos) cubeChunkPos;
            return this.x == chunkpos.x && this.y == chunkpos.y && this.z == chunkpos.z;
        }
    }

    /**
     * Get the first world X coordinate that belongs to this Chunk
     */
    public int getXStart() {
        return this.x << 4;
    }

    /**
     * Get the first world Y coordinate that belongs to this Chunk
     */
    public int getYStart() {
        return this.y << 4;
    }

    /**
     * Get the first world Z coordinate that belongs to this Chunk
     */
    public int getZStart() {
        return this.z << 4;
    }

    /**
     * Get the last world X coordinate that belongs to this Chunk
     */
    public int getXEnd() {
        return (this.x << 4) + 15;
    }


    /**
     * Get the last world Y coordinate that belongs to this Chunk
     */
    public int getYEnd() {
        return (this.y << 4) + 15;
    }

    /**
     * Get the last world Z coordinate that belongs to this Chunk
     */
    public int getZEnd() {
        return (this.z << 4) + 15;
    }

    /**
     * Gets the x-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordX() {
        return this.x >> 5;
    }

    /**
     * Gets the y-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordY() {
        return this.y >> 5;
    }

    /**
     * Gets the z-coordinate of the region file containing this chunk.
     */
    public int getRegionCoordZ() {
        return this.z >> 5;
    }

    /**
     * Gets the x-coordinate of this chunk within the region file that contains it.
     */
    public int getRegionPositionX() {
        return this.x & 31;
    }

    /**
     * Gets the z-coordinate of this chunk within the region file that contains it.
     */
    public int getRegionPositionZ() {
        return this.z & 31;
    }

    /**
     * Get the World coordinates of the Block with the given Chunk coordinates relative to this chunk
     */
    public BlockPos getBlock(int x, int y, int z) {
        return new BlockPos((this.x << 4) + x, (this.y << 4) + 4, (this.z << 4) + z);
    }

    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    public BlockPos asBlockPos() {
        return new BlockPos(this.x << 4, this.y << 4, this.z << 4);
    }

    public int getChessboardDistance(CubeChunkPos chunkPosIn) {
        return Math.max(Math.abs(this.x - chunkPosIn.x), Math.abs(this.z - chunkPosIn.z));
    }

    public static Stream<CubeChunkPos> getAllInBox(CubeChunkPos center, int radius) {
        return getAllInBox(new CubeChunkPos(center.x - radius, center.y  - radius, center.z - radius), new CubeChunkPos(center.x + radius,center.y  - radius, center.z + radius));
    }

    public static Stream<CubeChunkPos> getAllInBox(final CubeChunkPos start, final CubeChunkPos end) {
        int x = Math.abs(start.x - end.x) + 1;
        int y = Math.abs(start.y - end.y) + 1;
        int z = Math.abs(start.z - end.z) + 1;
        final int k = start.x < end.x ? 1 : -1;
        final int l = start.z < end.z ? 1 : -1;
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<CubeChunkPos>((long)(x * z), 64) {
            @Nullable
            private CubeChunkPos current;

            public boolean tryAdvance(Consumer<? super CubeChunkPos> p_tryAdvance_1_) {
                if (this.current == null) {
                    this.current = start;
                } else {
                    int currentX = this.current.x;
                    int currentY = this.current.y;
                    int currentZ = this.current.z;
                    if (currentX == end.x) {
                        if (currentZ == end.z) {
                            return false;
                        }

                        this.current = new CubeChunkPos(start.x, currentY, currentZ + l);
                    } else {
                        this.current = new CubeChunkPos(currentX + k, currentY, currentZ);
                    }
                }

                p_tryAdvance_1_.accept(this.current);
                return true;
            }
        }, false);
    }
}
