package cubicchunks.cc.chunk;

import cubicchunks.cc.chunk.cube.Cube;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;

import javax.annotation.Nullable;

public interface ICubeProvider {

    @Nullable
    ICube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load);

}
