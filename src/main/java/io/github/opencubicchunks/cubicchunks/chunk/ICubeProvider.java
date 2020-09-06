package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.chunk.ChunkStatus;

import javax.annotation.Nullable;

public interface ICubeProvider {

    @Nullable
    IBigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load);

}