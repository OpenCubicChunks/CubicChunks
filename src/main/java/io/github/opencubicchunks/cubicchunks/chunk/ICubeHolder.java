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
    // TODO: rename to match mojang names
    @Deprecated
    Either<IBigCube, ChunkHolder.IChunkLoadingError> MISSING_CUBE = unsafeCast(ChunkHolder.UNLOADED_CHUNK);
    @Deprecated
    Either<BigCube, ChunkHolder.IChunkLoadingError> UNLOADED_CUBE = unsafeCast(ChunkHolder.UNLOADED_LEVEL_CHUNK);
    @Deprecated
    CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> UNLOADED_CUBE_FUTURE = unsafeCast(ChunkHolderAccess.getUnloadedChunkFuture());
    @Deprecated
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> MISSING_CUBE_FUTURE = unsafeCast(ChunkHolder.UNLOADED_CHUNK_FUTURE);

    static ChunkStatus getCubeStatusFromLevel(int cubeLevel) {
        return cubeLevel < 33 ? ChunkStatus.FULL : CubeStatus.getStatus(cubeLevel - 33);
    }

    @Nullable
    BigCube getCubeIfComplete();

    CubePos getCubePos();

    // func_219276_a, getOrScheduleFuture
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getOrScheduleCubeFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getCubeFutureIfPresentUnchecked(ChunkStatus chunkStatus);

    CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> getCubeEntityTickingFuture();

    // func_219294_a, replaceProtoChunk
    void replaceProtoCube(CubePrimerWrapper primer);

    // func_225410_b, getFutureIfPresent
    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getCubeFutureIfPresent(ChunkStatus chunkStatus);

    void addCubeStageListener(ChunkStatus status, BiConsumer<Either<IBigCube, ChunkHolder.IChunkLoadingError>, Throwable> consumer, ChunkManager chunkManager);


    void broadcastChanges(BigCube cube);

    CompletableFuture<IBigCube> getCubeToSave();

    // added with ASM, can't be shadow because mixin validates shadows before preApply runs
    void updateCubeFutures(ChunkManager chunkManagerIn);

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