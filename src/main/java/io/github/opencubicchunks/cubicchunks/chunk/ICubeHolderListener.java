package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

public interface ICubeHolderListener {
    void onCubeLevelChange(CubePos pos, IntSupplier intSupplier, int p_219066_3_, IntConsumer p_219066_4_);
}