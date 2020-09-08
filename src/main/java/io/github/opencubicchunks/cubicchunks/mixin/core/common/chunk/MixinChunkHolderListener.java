package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolderListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

@Mixin(ChunkHolder.IListener.class)
public interface MixinChunkHolderListener extends ICubeHolderListener {

    @Override default void onCubeLevelChange(CubePos pos, IntSupplier intSupplier, int p_219066_3_, IntConsumer p_219066_4_) {
    }
}