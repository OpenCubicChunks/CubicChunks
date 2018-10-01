package io.github.opencubicchunks.cubicchunks.core.asm.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;

public interface IOptifineRenderChunk {
    ICube getCube();

    boolean isCubic();
}
