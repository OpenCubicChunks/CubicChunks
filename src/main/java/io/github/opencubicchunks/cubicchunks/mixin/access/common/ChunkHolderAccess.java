package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkHolder.class)
public interface ChunkHolderAccess {
    @Invoker("processUpdates") void processUpdatesCC(ChunkManager chunkManagerIn);
}
