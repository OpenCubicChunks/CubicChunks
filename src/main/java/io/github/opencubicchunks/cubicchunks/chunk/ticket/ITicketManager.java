package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.chunk.ChunkStatus;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.Executor;

public interface ITicketManager {
    int PLAYER_CUBE_TICKET_LEVEL = 33 + CubeStatus.getDistance(ChunkStatus.FULL) - 2;

    boolean processUpdates(ChunkMap chunkManager);

    <T> void addCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    <T> void removeCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    void addCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    void removeCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    // forceChunk
    void updateCubeForced(CubePos pos, boolean add);

    void addCubePlayer(CubePos cubePos, ServerPlayer player);

    void removeCubePlayer(CubePos cubePosIn, ServerPlayer player);

    int getNaturalSpawnCubeCount();

    boolean hasPlayersNearbyCube(long cubePosIn);

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getCubeTickets();

    Long2ObjectMap<ObjectSet<ServerPlayer>> getPlayersPerCube();

    ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getCubeTicketThrottlerInput();

    ProcessorHandle<CubeTaskPriorityQueueSorter.RunnableEntry> getCubeTicketThrottlerReleaser();

    LongSet getCubeTicketsToRelease();

    Set<ChunkHolder> getCubesToUpdateFutures();

    @Nullable
    ChunkHolder getCubeHolder(long chunkPosIn);

    @Nullable
    ChunkHolder updateCubeScheduling(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    boolean containsCubes(long cubePosIn);

    Executor getMainThreadExecutor();

    CubeTaskPriorityQueueSorter getCubeTicketThrottler();

    boolean hasCubePlayersNearby(long cubePos);
}