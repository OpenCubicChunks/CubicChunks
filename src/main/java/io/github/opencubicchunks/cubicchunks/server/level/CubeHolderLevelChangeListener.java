package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;

public interface CubeHolderLevelChangeListener {
    void onCubeLevelChange(CubePos pos, IntSupplier intSupplier, int p_219066_3_, IntConsumer p_219066_4_);
}