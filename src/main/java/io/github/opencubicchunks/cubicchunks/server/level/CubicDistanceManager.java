package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeStatus;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.chunk.ChunkStatus;

public interface CubicDistanceManager {
    int PLAYER_CUBE_TICKET_LEVEL = 33 + CubeStatus.getDistance(ChunkStatus.FULL) - 2;

    // TODO: remove these 2?
    boolean processUpdates(ChunkMap chunkManager);

    boolean runAllUpdatesForChunks(ChunkMap chunkMap);

    // addTicket
    <T> void addCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    // addTicket
    void addCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    // removeTicket
    <T> void removeCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    // removeTicket
    void removeCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    // addRegionTicket
    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    // removeRegionTicket
    <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    // updateChunkForced
    void updateCubeForced(CubePos pos, boolean add);

    // addPlayer
    void addCubePlayer(CubePos cubePos, ServerPlayer player);

    // removePlayer
    void removeCubePlayer(CubePos cubePosIn, ServerPlayer player);

    // getNaturalSpawnChunkCount
    int getNaturalSpawnCubeCount();

    // hasPlayersNearby
    boolean hasPlayersNearbyCube(long cubePosIn);

    // getTickets
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

    // hasPlayersNearby
    boolean hasCubePlayersNearby(long cubePos);

    //TODO: Is there a better way of figuring out if this world is generating chunks or cubes?!
    void initCubic(boolean world);

    // updatePlayerTickets
    void updatePlayerCubeTickets(int horizontalViewDistance, int verticalViewDistance);
}