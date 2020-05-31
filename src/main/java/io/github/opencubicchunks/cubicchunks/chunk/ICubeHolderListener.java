package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public interface ICubeHolderListener {
    void onUpdateCubeLevel(CubePos pos, IntSupplier intSupplier, int p_219066_3_, IntConsumer p_219066_4_);
}
