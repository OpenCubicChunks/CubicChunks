package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.level.chunk.ChunkAccess;

public interface ICubeAquifer {

    void prepareLocalWaterLevelForCube(ChunkAccess access);
}
