package io.github.opencubicchunks.cubicchunks.server.level.progress;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.chunk.ChunkStatus;

public interface CubeProgressListener extends ChunkProgressListener {
    void startCubes(CubePos center);

    void onCubeStatusChange(CubePos cubePos, @Nullable ChunkStatus newStatus);
    //Interface does not have a stopCubes(); because the equivalent stop for chunks does the same thing, and is called at the same time.
}