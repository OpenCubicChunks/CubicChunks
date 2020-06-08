package io.github.opencubicchunks.cubicchunks.chunk;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.server.ChunkHolder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CubeCollectorFuture extends CompletableFuture<List<Either<ICube, ChunkHolder.IChunkLoadingError>>> {

    private final int size;

    private AtomicInteger added = new AtomicInteger();

    private final Either<ICube, ChunkHolder.IChunkLoadingError>[] results;


    public CubeCollectorFuture(int size) {
        this.size = size;
        results = new Either[size];
    }


    public void add(int idx, Either<ICube, ChunkHolder.IChunkLoadingError> either, Throwable error) {
        if (error != null) {
            completeExceptionally(error);
        } else {
            results[idx] = either;
            added.getAndIncrement();
        }

        if (added.get() >= size) {
            this.complete(Arrays.asList(results));
        }
    }
}
