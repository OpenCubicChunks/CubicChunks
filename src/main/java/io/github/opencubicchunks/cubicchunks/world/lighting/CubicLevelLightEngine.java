package io.github.opencubicchunks.cubicchunks.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

public interface IWorldLightManager {

    void retainData(CubePos cubePos, boolean retain);

    void enableLightSources(CubePos cubePos, boolean retain);

}