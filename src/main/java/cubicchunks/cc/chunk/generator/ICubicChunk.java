package cubicchunks.cc.chunk.generator;

import net.minecraft.world.chunk.IChunk;

public interface ICubicChunk extends IChunk {

    /**
     * Gets a {@link CubeChunkPos} representing the x, y, and z coordinates of this chunk.
     */
    CubeChunkPos getCubePos();

}
