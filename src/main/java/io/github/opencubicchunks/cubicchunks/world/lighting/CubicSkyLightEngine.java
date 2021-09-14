package io.github.opencubicchunks.cubicchunks.world.lighting;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;

public interface CubicSkyLightEngine {
    void doSkyLightForCube(CubeAccess cube);
}
