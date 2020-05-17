package cubicchunks.cc.chunk.generator;

import cubicchunks.cc.chunk.graph.CubeChunkPos;
import net.minecraft.world.chunk.IChunk;

public interface ICubicChunk extends IChunk {

    /**
     * Gets a {@link CubeChunkPos} representing the x, y, and z coordinates of this chunk.
     * Not sure how we'll use this yet.
     */
    CubeChunkPos getCubePos();

}
