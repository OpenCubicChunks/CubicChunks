package cubicchunks.cc.mixin.core.client.progress;

import cubicchunks.cc.chunk.ISectionStatusListener;
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
public abstract class MixinChainedChunkStatusListener implements ISectionStatusListener {

    @Shadow @Final private DelegatedTaskExecutor<Runnable> executor;

    @Shadow @Final private IChunkStatusListener delegate;

    @Override public void startSections(SectionPos center) {
        this.executor.enqueue(() -> ((ISectionStatusListener) this.delegate).startSections(center));
    }

    @Override public void sectionStatusChanged(SectionPos chunkPosition, @Nullable ChunkStatus newStatus) {
        this.executor.enqueue(() -> ((ISectionStatusListener) this.delegate).sectionStatusChanged(chunkPosition, newStatus));
    }
}
