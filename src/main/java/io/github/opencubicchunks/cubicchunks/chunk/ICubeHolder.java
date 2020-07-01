package io.github.opencubicchunks.cubicchunks.chunk;

import static io.github.opencubicchunks.cubicchunks.chunk.util.Utils.unsafeCast;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkHolderAccess;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

public interface ICubeHolder {
    // TODO: all of their usages should be replaced with ASM
    @Deprecated
    Either<IBigCube, ChunkHolder.IChunkLoadingError> MISSING_CUBE = unsafeCast(ChunkHolder.MISSING_CHUNK);
    @Deprecated
    Either<BigCube, ChunkHolder.IChunkLoadingError> UNLOADED_CUBE = unsafeCast(ChunkHolder.UNLOADED_CHUNK);
    @Deprecated
    CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> UNLOADED_CUBE_FUTURE = unsafeCast(ChunkHolderAccess.getUnloadedChunkFuture());
    @Deprecated
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> MISSING_CUBE_FUTURE = unsafeCast(ChunkHolder.MISSING_CHUNK_FUTURE);

    static ChunkStatus getCubeStatusFromLevel(int cubeLevel) {
        return cubeLevel < 33 ? ChunkStatus.FULL : CubeStatus.getStatus(cubeLevel - 33);
    }

    @Nullable
    BigCube getCubeIfComplete();

    CubePos getCubePos();

    // func_219276_a
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getCubeFuture(ChunkStatus chunkStatus);

    CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> getCubeEntityTickingFuture();

    // func_219294_a
    void onCubeWrapperCreated(CubePrimerWrapper primer);

    // func_225410_b
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getFutureHigherThanCubeStatus(ChunkStatus chunkStatus);

    void addCubeStageListener(ChunkStatus status, BiConsumer<Either<IBigCube, ChunkHolder.IChunkLoadingError>, Throwable> consumer, ChunkManager chunkManager);


    void sendChanges(BigCube cube);

    CompletableFuture<IBigCube> getCurrentCubeFuture();

    // added with ASM, can't be shadow because mixin validates shadows before preApply runs
    void processCubeUpdates(ChunkManager chunkManagerIn);

    class CubeLoadingError implements ChunkHolder.IChunkLoadingError {
        private final ChunkHolder holder;

        public CubeLoadingError(ChunkHolder holder) {
            this.holder = holder;
        }

        @Override public String toString() {
            return "Unloaded ticket level " + ((ICubeHolder) holder).getCubePos().toString();
        }
    }
}
