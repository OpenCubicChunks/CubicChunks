package cubicchunks.cc.chunk.ticket;

import net.minecraft.world.server.ChunkHolder;

public interface IIntrinsicCCTicketManager {
    ChunkHolder getChunkHolder(long chunkPosIn);

    boolean contains(long p_219371_1_);
}