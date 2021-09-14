package io.github.opencubicchunks.cubicchunks.world.level.chunk;

import javax.annotation.Nullable;

import net.minecraft.world.level.chunk.ChunkStatus;

public interface CubeSource {

    @Nullable
    CubeAccess getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load);

}