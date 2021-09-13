package io.github.opencubicchunks.cubicchunks.levelgen.carver;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

public class CubicCarvingContext extends CarvingContext {

    private final ChunkAccess chunk;

    public CubicCarvingContext(ChunkGenerator generator, ChunkAccess chunk) {
        super(generator, chunk);
        this.chunk = chunk;
    }


    @Override public int getMinGenY() {
        return chunk.getMinBuildHeight();
    }

    @Override public int getGenDepth() {
        return chunk.getHeight();
    }

    public int getOriginalMinGenY() {
        return super.getMinGenY();
    }

    public int getOriginalGenDepth() {
        return super.getGenDepth();
    }
}
