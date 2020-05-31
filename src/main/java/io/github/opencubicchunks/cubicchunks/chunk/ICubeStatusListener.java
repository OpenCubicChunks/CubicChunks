package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.IChunkStatusListener;

import javax.annotation.Nullable;

public interface ICubeStatusListener extends IChunkStatusListener {

    default void startCubes(CubePos center) {
    }

    default void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
    }
}
