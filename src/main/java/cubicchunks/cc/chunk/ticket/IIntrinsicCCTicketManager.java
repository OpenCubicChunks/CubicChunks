package cubicchunks.cc.chunk.ticket;

import net.minecraft.world.server.ChunkHolder;

public interface IIntrinsicCCTicketManager {
    ChunkHolder getChunkHolder(long chunkPosIn);
}