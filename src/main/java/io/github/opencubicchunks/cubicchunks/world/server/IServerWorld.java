package io.github.opencubicchunks.cubicchunks.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;

public interface IServerWorld {
    void onCubeUnloading(BigCube cubeIn);

    void tickCube(BigCube cube, int randomTicks);
}