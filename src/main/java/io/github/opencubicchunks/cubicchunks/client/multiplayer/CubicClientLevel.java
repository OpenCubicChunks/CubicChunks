package io.github.opencubicchunks.cubicchunks.client.multiplayer;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;

public interface CubicClientLevel {

    void onCubeLoaded(int cubeX, int cubeY, int cubeZ);

    void onCubeUnload(LevelCube cube);
}