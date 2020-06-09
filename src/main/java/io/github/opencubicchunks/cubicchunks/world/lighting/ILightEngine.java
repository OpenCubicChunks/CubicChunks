package io.github.opencubicchunks.cubicchunks.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

public interface ILightEngine {
    void retainCubeData(CubePos pos, boolean retain);

    void func_215620_a(CubePos p_215620_1_, boolean p_215620_2_);
}
