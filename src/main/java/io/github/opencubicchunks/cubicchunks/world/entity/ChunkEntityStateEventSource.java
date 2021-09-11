package io.github.opencubicchunks.cubicchunks.world.entity;

public interface ChunkEntityStateEventSource {
    void registerChunkEntityStateEventHandler(ChunkEntityStateEventHandler handler);
}
