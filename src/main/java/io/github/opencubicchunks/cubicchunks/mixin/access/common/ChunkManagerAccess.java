package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkManagerAccess {

    @Invoker ChunkHolder invokeGetUpdatingChunkIfPresent(long chunkPosIn);
    @Invoker boolean invokePromoteChunkMap();
    @Invoker void invokeOnFullChunkStatusChange(ChunkPos chunkPos, ChunkHolder.FullChunkStatus fullChunkStatus);
    @Accessor int getViewDistance();
}