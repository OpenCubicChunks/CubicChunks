package cubicchunks.cc.mixin.core.client.progress;

import cubicchunks.cc.chunk.ICubeStatusListener;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.math.SectionPos;
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

    @Override public void startSections(SectionPos center) {
        this.executor.enqueue(() -> ((ICubeStatusListener) this.delegate).startSections(center));
    }

    @Override public void cubeStatusChanged(SectionPos chunkPosition, @Nullable ChunkStatus newStatus) {
        this.executor.enqueue(() -> ((ICubeStatusListener) this.delegate).cubeStatusChanged(chunkPosition, newStatus));
    }
}
