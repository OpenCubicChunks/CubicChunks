package cubicchunks.cc.mixin.core.common.chunk;

import cubicchunks.cc.chunk.IChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

public class MixinChunkSection implements IChunkSection {

    private ChunkStatus status = ChunkStatus.EMPTY;

    @Override public ChunkStatus getStatus() {
        return status;
    }
}
