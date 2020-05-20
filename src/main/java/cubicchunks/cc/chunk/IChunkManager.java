package cubicchunks.cc.chunk;

import net.minecraft.world.server.ChunkHolder;

import javax.annotation.Nullable;

public interface IChunkManager {
    ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);
}
