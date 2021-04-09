package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Objects;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.ChunkPos;

/**
 * A cube pos presented as a ChunkPos making it eligible in all use cases methods with hardcoded ChunkPos parameters
 * <p>
 * Currently only used to assist in the Cubic(3D) entity tracker + storage.
 */
public class ImposterChunkPos extends ChunkPos {
    public final int y;

    public ImposterChunkPos(int x, int y, int z) {
        super(x, z);
        this.y = y;
    }

    public ImposterChunkPos(CubePos cubePos) {
        this(cubePos.getX(), cubePos.getY(), cubePos.getZ());
    }

    public CubePos toCubePos() {
        return CubePos.of(this.x, this.y, this.z);
    }

    @Override public long toLong() {
        return toCubePos().asLong();
    }

    @Override public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImposterChunkPos that = (ImposterChunkPos) o;
        return y == that.y;
    }

    @Override public int hashCode() {
        return Objects.hash(super.hashCode(), y);
    }

    @Override public int getChessboardDistance(ChunkPos pos) {
        if (pos instanceof ImposterChunkPos) {
            return Math.max(super.getChessboardDistance(pos), Math.abs(this.y - ((ImposterChunkPos) pos).y));
        }
        return super.getChessboardDistance(pos);
    }
}