package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

public class EmptyCube extends Cube {

    public EmptyCube(World worldIn) {
        super(worldIn, CubePos.of(0, 0, 0), null);
    }

    @Override public ChunkSection[] getCubeSections() {
        throw new UnsupportedOperationException("This is empty cube!");
    }
}
