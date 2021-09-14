package io.github.opencubicchunks.cubicchunks.world.lighting;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;

public interface CubicLevelLightEngine {

    void retainData(CubePos cubePos, boolean retain);

    void enableLightSources(CubePos cubePos, boolean retain);

}