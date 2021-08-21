package io.github.opencubicchunks.cubicchunks.world.entity;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

public interface ChunkEntityStateEventHandler {
    void onCubeEntitiesLoad(CubePos pos);

    void onCubeEntitiesUnload(CubePos pos);
}
