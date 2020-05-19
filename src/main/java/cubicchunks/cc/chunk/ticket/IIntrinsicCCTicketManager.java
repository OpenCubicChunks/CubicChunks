package cubicchunks.cc.chunk.ticket;

import net.minecraft.world.server.ChunkHolder;

import javax.annotation.Nullable;

public interface IIntrinsicCCTicketManager {
    ChunkHolder getChunkHolder(long chunkPosIn);
    ChunkHolder setChunkLevel(long chunkPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);
    boolean contains(long p_219371_1_);
}