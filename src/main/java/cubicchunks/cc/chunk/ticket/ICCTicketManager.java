package cubicchunks.cc.chunk.ticket;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

public interface ICCTicketManager {
    boolean processUpdates(ChunkManager chunkManager);

    <T> void registerWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void releaseWithLevel(TicketType<T> type, SectionPos pos, int level, T value);

    <T> void register(TicketType<T> type, SectionPos pos, int distance, T value);

    <T> void release(TicketType<T> type, SectionPos pos, int distance, T value);

    void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player);

    void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player);

    int getSpawningChunksCount();

    boolean isOutsideSpawningRadius(long sectionPosIn);

    String func_225412_c();

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTickets();

    ChunkHolder func_219335_b(long chunkPosIn);

    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getplayersByChunkPos();


}
