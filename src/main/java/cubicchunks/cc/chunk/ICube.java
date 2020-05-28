package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;

public interface ICube {
    SectionPos getSectionPos();

    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();
}
