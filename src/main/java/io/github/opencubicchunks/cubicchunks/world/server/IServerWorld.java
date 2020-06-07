package io.github.opencubicchunks.cubicchunks.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;

public interface IServerWorld {
    void onCubeUnloading(Cube cubeIn);
}
