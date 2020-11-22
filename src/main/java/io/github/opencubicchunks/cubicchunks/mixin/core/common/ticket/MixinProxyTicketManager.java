package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.DistanceManager.class)
public abstract class MixinProxyTicketManager extends MixinTicketManager {

    // Mixin AP doesn't see the field, we need to provide intermediary name explicitly
    @SuppressWarnings("target") @Shadow(aliases = "field_17443", remap = false) ChunkMap this$0;

    @Override
    public boolean containsCubes(long cubePosIn) {
        return ((IChunkManager) this$0).getCubesToDrop().contains(cubePosIn);
    }

    @Override
    @Nullable
    public ChunkHolder getCubeHolder(long cubePosIn) {
        return ((IChunkManager) this$0).getCubeHolder(cubePosIn);
    }
}