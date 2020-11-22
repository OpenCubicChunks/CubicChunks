package io.github.opencubicchunks.cubicchunks.chunk;

import javax.annotation.Nullable;

import net.minecraft.world.level.chunk.ChunkStatus;

public interface ICubeProvider {

    @Nullable
    IBigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load);

}