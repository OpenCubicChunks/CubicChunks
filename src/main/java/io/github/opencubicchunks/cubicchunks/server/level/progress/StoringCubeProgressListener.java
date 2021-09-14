package io.github.opencubicchunks.cubicchunks.server.level.progress;

import javax.annotation.Nullable;

import net.minecraft.world.level.chunk.ChunkStatus;

public interface StoringCubeProgressListener {

    @Nullable ChunkStatus getCubeStatus(int x, int y, int z);
}