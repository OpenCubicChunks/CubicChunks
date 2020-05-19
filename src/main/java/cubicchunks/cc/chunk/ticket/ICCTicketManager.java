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

import java.util.concurrent.Executor;

public interface ICCTicketManager {
    int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;

    boolean processUpdates(ChunkManager chunkManager);

    <T> void registerWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void releaseWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void register(TicketType<T> type, SectionPos pos, int distance, T value);

    void cc$register(long chunkPosIn, Ticket<?> ticketIn);

    void cc$release(long chunkPosIn, Ticket<?> ticketIn);

    <T> void release(TicketType<T> type, SectionPos pos, int distance, T value);

    void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player);

    void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player);

    int getSpawningChunksCount();

    boolean isOutsideSpawningRadius(long sectionPosIn);

    String func_225412_c();

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTickets();

    //TODO: figure out if this is actually supposed to be here
    //ChunkHolder func_219335_b(long chunkPosIn);

    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getplayersByChunkPos();

    ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getPlayerTicketThrottler();

    ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getplayerTicketThrottlerSorter();

    LongSet getChunkPositions();

    Executor executor();

    CubeTaskPriorityQueueSorter getlevelUpdateListener();
}