package io.github.opencubicchunks.cubicchunks.world.lighting;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;

public interface CubicLayerLightEngine {
    void retainCubeData(CubePos pos, boolean retain);

    void enableLightSources(CubePos cubePos, boolean enable);
}