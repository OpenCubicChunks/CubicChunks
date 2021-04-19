package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
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

public interface ITicketManager {
    int PLAYER_CUBE_TICKET_LEVEL = 33 + CubeStatus.getDistance(ChunkStatus.FULL) - 2;

    boolean processUpdates(ChunkMap chunkManager);

    boolean runAllUpdatesForChunks(ChunkMap chunkMap);

    <T> void addCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    void addCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    <T> void removeCubeTicket(TicketType<T> type, CubePos pos, int level, T value);

    void removeCubeTicket(long chunkPosIn, Ticket<?> ticketIn);

    <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

    <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value);

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

    //TODO: Is there a better way of figuring out if this world is generating chunks or cubes?!
    void hasCubicTickets(boolean world);

    void updatePlayerCubeTickets(int horizontalViewDistance, int verticalViewDistance);
}