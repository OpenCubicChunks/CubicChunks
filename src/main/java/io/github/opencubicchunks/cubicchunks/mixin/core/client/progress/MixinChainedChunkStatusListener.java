package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.ChainedChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ChainedChunkStatusListener.class)
public abstract class MixinChainedChunkStatusListener implements ICubeStatusListener {

    @Shadow @Final private DelegatedTaskExecutor<Runnable> executor;

    @Shadow @Final private IChunkStatusListener delegate;

    @Override public void startCubes(CubePos center) {
        this.executor.enqueue(() -> ((ICubeStatusListener) this.delegate).startCubes(center));
    }

    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        this.executor.enqueue(() -> ((ICubeStatusListener) this.delegate).cubeStatusChanged(cubePos, newStatus));
    }
}
