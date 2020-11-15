package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.level.chunk.ChunkStatus;

import javax.annotation.Nullable;

public interface ITrackingCubeStatusListener {

    @Nullable ChunkStatus getCubeStatus(int x, int y, int z);
}