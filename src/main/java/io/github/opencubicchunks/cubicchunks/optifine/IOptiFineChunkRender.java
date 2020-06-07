package io.github.opencubicchunks.cubicchunks.optifine;

import net.minecraft.world.chunk.ChunkSection;

public interface IOptiFineChunkRender {
    ChunkSection getCube();

    int getRegionX();

    int getRegionY();
}
