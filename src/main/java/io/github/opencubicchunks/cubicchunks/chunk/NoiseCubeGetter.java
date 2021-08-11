package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

public interface NoiseCubeGetter {

    Long2ObjectArrayMap<CubePrimer> getNoisePrimers();
}
