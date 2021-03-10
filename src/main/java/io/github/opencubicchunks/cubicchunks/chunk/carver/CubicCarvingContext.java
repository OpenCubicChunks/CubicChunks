package io.github.opencubicchunks.cubicchunks.chunk.carver;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

public class CubicCarvingContext extends CarvingContext {

    private final ChunkAccess chunk;

    public CubicCarvingContext(ChunkAccess chunk) {
        super(null);
        this.chunk = chunk;
    }


    @Override public int getMinGenY() {
        return chunk.getMinBuildHeight();
    }

    @Override public int getGenDepth() {
        return chunk.getHeight();
    }
}
