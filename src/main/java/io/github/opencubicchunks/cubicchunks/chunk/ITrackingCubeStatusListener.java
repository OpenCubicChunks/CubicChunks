package io.github.opencubicchunks.cubicchunks.chunk;

import javax.annotation.Nullable;

import net.minecraft.world.level.chunk.ChunkStatus;

public interface ITrackingCubeStatusListener {

    @Nullable ChunkStatus getCubeStatus(int x, int y, int z);
}