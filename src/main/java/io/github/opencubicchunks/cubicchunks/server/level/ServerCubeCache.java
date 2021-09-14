package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeSource;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public interface ServerCubeCache extends CubeSource {
    // TODO check whether this is still needed
    // ChunkHolder getChunkHolderForce(ChunkPos chunkPos, ChunkStatus requiredStatus);

    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    int getTickingGeneratedCubes();

    void forceCube(CubePos pos, boolean add);

    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getColumnFutureForCube(CubePos cubePos, int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);
    // TODO check whether this is still needed
    // boolean isEntityTickingCube(CubePos pos);

    boolean checkCubeFuture(long cubePosLong, Function<ChunkHolder, CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>>> futureFunction);
}
