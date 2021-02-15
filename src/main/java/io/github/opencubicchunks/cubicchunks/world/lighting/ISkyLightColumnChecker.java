package io.github.opencubicchunks.cubicchunks.world.lighting;

import net.minecraft.world.level.chunk.LevelChunk;

public interface ISkyLightColumnChecker {
    void checkSkyLightColumn(LevelChunk chunk, int x, int z, int oldHeight, int newHeight);
}
