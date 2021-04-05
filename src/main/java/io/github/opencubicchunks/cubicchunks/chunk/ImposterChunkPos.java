package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.ChunkPos;

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
}