package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.access;

import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkProvider.class)
public interface ClientChunkProviderAccess {
    @Accessor ClientChunkProvider.ChunkArray getArray();
}
