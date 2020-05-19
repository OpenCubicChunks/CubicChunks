package cubicchunks.cc.chunk.ticket;

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

    <T> void registerWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void releaseWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void register(TicketType<T> type, SectionPos pos, int distance, T value);

    void cc$register(long chunkPosIn, Ticket<?> ticketIn);

    <T> void release(TicketType<T> type, SectionPos pos, int distance, T value);

    void cc$release(long chunkPosIn, Ticket<?> ticketIn);

    void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player);

    void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player);

    int getSpawningChunksCount();

    boolean isOutsideSpawningRadius(long sectionPosIn);

    String func_225412_c();

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTickets();

    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getplayersByChunkPos();

    ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getCc$playerTicketThrottler();

    ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getplayerTicketThrottlerSorter();

    LongSet getChunkPositions();

    Set<ChunkHolder> getChunkHolders();

    ChunkHolder cc$getChunkHolder(long chunkPosIn);

    ChunkHolder cc$setChunkLevel(long chunkPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    boolean cc$contains(long p_219371_1_);

    Executor executor();

    CubeTaskPriorityQueueSorter getlevelUpdateListener();
}