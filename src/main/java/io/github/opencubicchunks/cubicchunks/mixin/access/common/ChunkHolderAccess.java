package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkHolder.class)
public interface ChunkHolderAccess {
    @Accessor("UNLOADED_CHUNK_FUTURE") static CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> getUnloadedChunkFuture() {
        throw new Error("Mixin failed to apply");
    }

    @Invoker("processUpdates") void processUpdatesCC(ChunkManager chunkManagerIn);
}