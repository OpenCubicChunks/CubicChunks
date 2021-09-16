package io.github.opencubicchunks.cubicchunks.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class DummyHeightmap extends Heightmap {
    public DummyHeightmap(ChunkAccess chunkAccess, Types types) {
        super(chunkAccess, types);
    }

    @Override public boolean update(int x, int y, int z, BlockState state) {
        return false;
    }

    @Override public int getFirstAvailable(int x, int z) {
        return 0;
    }

    @Override public void setRawData(ChunkAccess clv, Types a, long[] heightmap) {
    }

    @Override public long[] getRawData() {
       return new long[0];
    }
}
