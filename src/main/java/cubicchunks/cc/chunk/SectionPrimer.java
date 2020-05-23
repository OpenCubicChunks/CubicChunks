package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;

import javax.annotation.Nullable;

public class SectionPrimer implements ISection {

    private ChunkSection section;

    //TODO: add TickList<Block> and TickList<Fluid>
    public SectionPrimer(SectionPos pos, @Nullable ChunkSection sectionIn)
    {
        if(sectionIn == null) {
            this.section = new ChunkSection(pos.getY(), (short)0, (short)0, (short)0);
        }
        else {
            this.section = sectionIn;
        }
    }

    public ChunkSection getSection() {
        return this.section;
    }

}
