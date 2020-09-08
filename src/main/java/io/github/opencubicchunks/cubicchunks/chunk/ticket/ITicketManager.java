package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

public interface ITicketManager {
    int PLAYER_CUBE_TICKET_LEVEL = 33 + CubeStatus.getDistance(ChunkStatus.FULL) - 2;

    boolean processUpdates(ChunkManager chunkManager);

    <T> void addCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    <T> void removeCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    void addCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    void removeCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    // forceChunk
    void updateCubeForced(CubePos pos, boolean add);

    void addCubePlayer(CubePos cubePos, ServerPlayerEntity player);

    void removeCubePlayer(CubePos cubePosIn, ServerPlayerEntity player);

    int getNaturalSpawnCubeCount();

    boolean hasPlayersNearbyCube(long cubePosIn);

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getCubeTickets();

    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersPerCube();

    ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getCubeTicketThrottlerInput();

    ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getCubeTicketThrottlerReleaser();

    LongSet getCubeTicketsToRelease();

    Set<ChunkHolder> getCubesToUpdateFutures();

    @Nullable
    ChunkHolder getCubeHolder(long chunkPosIn);

    @Nullable
    ChunkHolder updateCubeScheduling(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    boolean containsCubes(long cubePosIn);

    Executor getMainThreadExecutor();

    CubeTaskPriorityQueueSorter getCubeTicketThrottler();
}