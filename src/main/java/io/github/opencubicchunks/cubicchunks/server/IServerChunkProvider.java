package io.github.opencubicchunks.cubicchunks.server;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.TicketType;

public interface IServerChunkProvider extends ICubeProvider {
    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    int getTickingGeneratedCubes();

    void forceCube(CubePos pos, boolean add);

    boolean isEntityTickingCube(CubePos pos);

    boolean checkCubeFuture(long cubePosLong, Function<ChunkHolder, CompletableFuture<Either<BigCube, ChunkHolder.ChunkLoadingFailure>>> futureFunction);
}
