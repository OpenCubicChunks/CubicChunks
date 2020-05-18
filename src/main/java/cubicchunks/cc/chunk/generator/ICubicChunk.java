package cubicchunks.cc.chunk.generator;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.IChunk;

public interface ICubicChunk extends IChunk {

    /**
     * Gets a {@link SectionPos} representing the x, y, and z coordinates of this chunk.
     * Not sure how we'll use this yet.
     */
    SectionPos getSectionPos();

}
