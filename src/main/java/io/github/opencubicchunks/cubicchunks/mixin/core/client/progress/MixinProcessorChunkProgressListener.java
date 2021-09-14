package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.server.level.progress.CubeProgressListener;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProcessorChunkProgressListener.class)
public abstract class MixinProcessorChunkProgressListener implements CubeProgressListener {

    @Shadow @Final private ProcessorMailbox<Runnable> mailbox;

    @Shadow @Final private ChunkProgressListener delegate;

    @Override public void startCubes(CubePos center) {
        this.mailbox.tell(() -> ((CubeProgressListener) this.delegate).startCubes(center));
    }

    @Override public void onCubeStatusChange(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        this.mailbox.tell(() -> ((CubeProgressListener) this.delegate).onCubeStatusChange(cubePos, newStatus));
    }
}