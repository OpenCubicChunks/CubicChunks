package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;


public interface ISectionHolder {
    ChunkSection getChunkIfComplete();

    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();
}
