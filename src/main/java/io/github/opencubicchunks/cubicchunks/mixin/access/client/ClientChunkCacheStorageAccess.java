package io.github.opencubicchunks.cubicchunks.mixin.access.client;

import java.util.concurrent.atomic.AtomicReferenceArray;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface ClientChunkCacheStorageAccess {

    @Invoker boolean invokeInRange(int x, int z);
    @Invoker int invokeGetIndex(int x, int z);
    @Accessor AtomicReferenceArray<LevelChunk> getChunks();
    @Invoker void invokeReplace(int columnIdx, LevelChunk chunk);
}