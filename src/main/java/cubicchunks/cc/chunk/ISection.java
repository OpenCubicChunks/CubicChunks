package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;

public interface ISection {
    SectionPos getSectionPos();

    void setSectionStatus(ChunkStatus status);
    ChunkStatus getSectionStatus();
}
