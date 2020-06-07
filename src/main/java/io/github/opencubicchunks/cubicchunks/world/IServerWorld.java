package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;

public interface IServerWorld {
    void onCubeUnloading(Cube cubeIn);
}
