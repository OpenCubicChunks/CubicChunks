package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.IChunkStatusListener;

import javax.annotation.Nullable;

public interface ICubeStatusListener extends IChunkStatusListener {
    void startCubes(CubePos center);

    void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus);
    //Interface does not have a stopCubes(); because the equivalent stop for chunks does the same thing, and is called at the same time.
}
