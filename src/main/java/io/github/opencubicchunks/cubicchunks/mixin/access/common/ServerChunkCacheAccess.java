package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheAccess {
    @Invoker boolean invokeChunkAbsent(@Nullable ChunkHolder holder, int maxLevel);
    @Invoker boolean invokeRunDistanceManagerUpdates();
}
