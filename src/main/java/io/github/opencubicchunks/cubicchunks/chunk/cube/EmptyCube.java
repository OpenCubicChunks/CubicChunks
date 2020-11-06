package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class EmptyCube extends BigCube {

    public EmptyCube(Level worldIn) {
        super(worldIn, CubePos.of(0, 0, 0), null);
    }

    @Override public LevelChunkSection[] getCubeSections() {
        throw new UnsupportedOperationException("This is empty cube!");
    }
}