package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;

public interface ISectionHolder {
    ChunkSection getSectionIfComplete();

    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();
}
