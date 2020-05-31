package cubicchunks.cc.chunk.cube;

import cubicchunks.cc.chunk.biome.CubeBiomeContainer;
import cubicchunks.cc.chunk.util.CubePos;
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
