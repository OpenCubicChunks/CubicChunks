package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.Executor;

public interface ITicketManager {
    int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;

    boolean processUpdates(ChunkManager chunkManager);

    <T> void registerWithLevel(TicketType<T> type, CubePos pos, int level, T value);

    <T> void releaseWithLevel(TicketType<T> type, CubePos pos, int level, T value);

    <T> void register(TicketType<T> type, CubePos pos, int distance, T value);

    void registerSection(long chunkPosIn, Ticket<?> ticketIn);

    <T> void release(TicketType<T> type, CubePos pos, int distance, T value);

    void releaseCube(long chunkPosIn, Ticket<?> ticketIn);



    void removePlayer(CubePos cubePosIn, ServerPlayerEntity player);

    int getSpawningSectionsCount();

    boolean isSectionOutsideSpawningRadius(long cubePosIn);

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getCubeTickets();

    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersBySectionPos();

    ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getSectionPlayerTicketThrottler();

    ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getPlayerSectionTicketThrottlerSorter();

    LongSet getSectionPositions();

    Set<ChunkHolder> getCubeHolders();

    ChunkHolder getCubeHolder(long chunkPosIn);

    ChunkHolder setCubeLevel(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    boolean containsCubes(long sectionPos);

    Executor executor();

    CubeTaskPriorityQueueSorter getCubeTaskPriorityQueueSorter();
}