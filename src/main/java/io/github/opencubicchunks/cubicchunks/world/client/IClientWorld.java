package io.github.opencubicchunks.cubicchunks.world.client;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;

public interface IClientWorld {

    void onCubeLoaded(int cubeX, int cubeY, int cubeZ);

    void onCubeUnload(BigCube cube);
}