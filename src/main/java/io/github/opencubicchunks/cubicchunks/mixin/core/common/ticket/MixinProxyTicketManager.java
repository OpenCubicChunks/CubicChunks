package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ChunkManager.ProxyTicketManager.class)
public abstract class MixinProxyTicketManager extends MixinTicketManager {

    // this$0 has an SRG name, but it's not actually used at runtime, but FG will remap it anyway. This alias gets around that remapping.
    @SuppressWarnings("ShadowTarget") @Shadow(aliases = "this$0") ChunkManager syntheticThis;

    @Override
    public boolean containsCubes(long cubePosIn) {
        return ((IChunkManager) syntheticThis).getCubesToDrop().contains(cubePosIn);
    }

    @Override
    @Nullable
    public ChunkHolder getCubeHolder(long cubePosIn) {
        return ((IChunkManager) syntheticThis).getCubeHolder(cubePosIn);
    }
}