package io.github.opencubicchunks.cubicchunks.mixin.access.client;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkProvider$ChunkArray")
public interface ClientChunkProviderChunkArrayAccess {

    @Invoker boolean invokeInRange(int x, int z);
    @Invoker int invokeGetIndex(int x, int z);
    @Accessor AtomicReferenceArray<Chunk> getChunks();
    @Invoker void invokeReplace(int columnIdx, Chunk chunk);
}