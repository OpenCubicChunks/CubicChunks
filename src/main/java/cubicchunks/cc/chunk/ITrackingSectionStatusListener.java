package cubicchunks.cc.chunk;

import net.minecraft.world.chunk.ChunkStatus;

public interface ITrackingSectionStatusListener {

    ChunkStatus getSectionStatus(int x, int y, int z);
}
