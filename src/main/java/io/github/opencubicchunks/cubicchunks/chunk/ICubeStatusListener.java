package cubicchunks.cc.chunk;

import cubicchunks.cc.chunk.util.CubePos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.IChunkStatusListener;

import javax.annotation.Nullable;

public interface ICubeStatusListener extends IChunkStatusListener {

    default void startCubes(CubePos center) {
    }

    default void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
    }
}
