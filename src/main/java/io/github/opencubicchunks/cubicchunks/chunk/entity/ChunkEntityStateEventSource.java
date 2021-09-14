package io.github.opencubicchunks.cubicchunks.chunk.entity;

public interface ChunkEntityStateEventSource {
    void registerChunkEntityStateEventHandler(ChunkEntityStateEventHandler handler);
}
