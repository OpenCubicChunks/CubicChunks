package io.github.opencubicchunks.cubicchunks.chunk.entity;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;

public interface ChunkEntityStateEventHandler {
    void onCubeEntitiesLoad(CubePos pos);

    void onCubeEntitiesUnload(CubePos pos);
}
