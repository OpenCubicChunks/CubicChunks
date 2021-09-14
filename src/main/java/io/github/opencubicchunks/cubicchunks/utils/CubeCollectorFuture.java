package io.github.opencubicchunks.cubicchunks.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import net.minecraft.server.level.ChunkHolder;

public class CubeCollectorFuture extends CompletableFuture<List<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>>> {

    private final int size;

    private AtomicInteger added = new AtomicInteger();

    private final Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>[] results;


    public CubeCollectorFuture(int size) {
        this.size = size;
        results = new Either[size];
    }


    public void add(int idx, Either<CubeAccess, ChunkHolder.ChunkLoadingFailure> either, Throwable error) {
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