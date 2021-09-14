package io.github.opencubicchunks.cubicchunks.world.level.chunk;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class EmptyLevelCube extends LevelCube {

    public EmptyLevelCube(Level worldIn) {
        super(worldIn, CubePos.of(0, 0, 0), null);
    }

    @Override public LevelChunkSection[] getCubeSections() {
        throw new UnsupportedOperationException("This is empty cube!");
    }
}