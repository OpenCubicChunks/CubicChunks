package io.github.opencubicchunks.cubicchunks.optifine;

import net.minecraft.world.level.chunk.LevelChunkSection;

public interface IOptiFineChunkRender {
    LevelChunkSection getCube();

    int getRegionX();

    int getRegionY();
}