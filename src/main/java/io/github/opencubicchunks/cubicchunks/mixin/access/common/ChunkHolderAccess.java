package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkHolder.class)
public interface ChunkHolderAccess {
    @Accessor("UNLOADED_CHUNK_FUTURE") static CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getUnloadedChunkFuture() {
        throw new Error("Mixin failed to apply");
    }

    @Invoker void invokeUpdateFutures(ChunkMap chunkManagerIn, Executor exec);
}