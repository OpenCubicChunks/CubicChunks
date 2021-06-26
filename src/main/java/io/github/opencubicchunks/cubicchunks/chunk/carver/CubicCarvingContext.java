package io.github.opencubicchunks.cubicchunks.chunk.carver;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

public class CubicCarvingContext extends CarvingContext {

    private final ChunkAccess chunk;
    private final int minY;
    private final int maxY;

    public CubicCarvingContext(ChunkGenerator generator, ChunkAccess chunk) {
        super(generator);
        this.chunk = chunk;
        this.minY = chunk.getMinBuildHeight() + 8;
        this.maxY = IBigCube.DIAMETER_IN_BLOCKS + 8;
    }

    @Override public int getMinGenY() {
        return minY;
    }

    @Override public int getGenDepth() {
        return maxY;
    }

    public int getOriginalMinGenY() {
        return super.getMinGenY();
    }

    public int getOriginalGenDepth() {
        return super.getGenDepth();
    }
}
