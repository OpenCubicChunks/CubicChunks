package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;

public interface ICubeHolder {
    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();
}
