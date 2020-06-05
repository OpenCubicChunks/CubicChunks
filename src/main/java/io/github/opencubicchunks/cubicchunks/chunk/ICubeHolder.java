package io.github.opencubicchunks.cubicchunks.chunk;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

public interface ICubeHolder {
    Either<ICube, ChunkHolder.IChunkLoadingError> MISSING_CUBE = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    Either<Cube, ChunkHolder.IChunkLoadingError> UNLOADED_CUBE = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> UNLOADED_CUBE_FUTURE = CompletableFuture.completedFuture(UNLOADED_CUBE);
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> MISSING_CUBE_FUTURE = CompletableFuture.completedFuture(MISSING_CUBE);

    static ChunkStatus getCubeStatusFromLevel(int cubeLevel) {
        return cubeLevel < 33 ? ChunkStatus.FULL : CubeStatus.getStatus(cubeLevel - 33);
    }

    @Nullable
    Cube getCubeIfComplete();

    CubePos getCubePos();

    void chainCube(CompletableFuture<? extends Either<? extends ICube,
            ChunkHolder.IChunkLoadingError>> eitherChunk);

    // func_219276_a
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getCubeFuture(ChunkStatus chunkStatus);

    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> getCubeEntityTickingFuture();

    // func_219294_a
    void onCubeWrapperCreated(CubePrimerWrapper primer);

    // func_225410_b
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getFutureHigherThanCubeStatus(ChunkStatus chunkStatus);


    void sendChanges(Cube cube);

    CompletableFuture<ICube> getCurrentCubeFuture();
}
