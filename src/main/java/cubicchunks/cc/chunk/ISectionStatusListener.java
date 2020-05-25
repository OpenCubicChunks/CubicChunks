package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.IChunkStatusListener;

import javax.annotation.Nullable;

public interface ISectionStatusListener extends IChunkStatusListener {

    default void startSections(SectionPos center) {
    }

    default void sectionStatusChanged(SectionPos chunkPosition, @Nullable ChunkStatus newStatus) {
    }
}
