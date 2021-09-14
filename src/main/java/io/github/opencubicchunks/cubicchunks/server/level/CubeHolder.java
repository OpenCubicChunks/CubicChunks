package io.github.opencubicchunks.cubicchunks.server.level;

import static io.github.opencubicchunks.cubicchunks.utils.Utils.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeStatus;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ImposterProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkHolderAccess;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkStatus;

public interface CubeHolder {
    // TODO: all of their usages should be replaced with ASM
    // TODO: rename to match mojang names
    @Deprecated
    Either<CubeAccess, ChunkHolder.ChunkLoadingFailure> MISSING_CUBE = unsafeCast(ChunkHolder.UNLOADED_CHUNK);
    @Deprecated
    Either<LevelCube, ChunkHolder.ChunkLoadingFailure> UNLOADED_CUBE = unsafeCast(ChunkHolder.UNLOADED_LEVEL_CHUNK);
    @Deprecated
    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CUBE_FUTURE = unsafeCast(ChunkHolderAccess.getUnloadedChunkFuture());
    @Deprecated
    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> MISSING_CUBE_FUTURE = unsafeCast(ChunkHolder.UNLOADED_CHUNK_FUTURE);

    static ChunkStatus getCubeStatusFromLevel(int cubeLevel) {
        return cubeLevel < 33 ? ChunkStatus.FULL : CubeStatus.getStatus(cubeLevel - 33);
    }

    @Nullable
    LevelCube getCubeIfComplete();

    CubePos getCubePos();

    // func_219276_a, getOrScheduleFuture
    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleCubeFuture(ChunkStatus chunkStatus, ChunkMap chunkManager);

    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> getCubeFutureIfPresentUnchecked(ChunkStatus chunkStatus);

    CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> getCubeEntityTickingFuture();

    // func_219294_a, replaceProtoChunk
    void replaceProtoCube(ImposterProtoCube primer);

    // func_225410_b, getFutureIfPresent
    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> getCubeFutureIfPresent(ChunkStatus chunkStatus);

    void addCubeStageListener(ChunkStatus status, BiConsumer<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>, Throwable> consumer, ChunkMap chunkManager);


    void broadcastChanges(LevelCube cube);

    CompletableFuture<CubeAccess> getCubeToSave();

    // added with ASM, can't be shadow because mixin validates shadows before preApply runs
    void updateCubeFutures(ChunkMap chunkManagerIn, Executor executor);

    class CubeLoadingError implements ChunkHolder.ChunkLoadingFailure {
        private final ChunkHolder holder;

        public CubeLoadingError(ChunkHolder holder) {
            this.holder = holder;
        }

        @Override public String toString() {
            return "Unloaded ticket level " + ((CubeHolder) holder).getCubePos().toString();
        }
    }
}