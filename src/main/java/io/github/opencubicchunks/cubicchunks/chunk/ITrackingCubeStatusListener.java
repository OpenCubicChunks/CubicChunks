package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.chunk.ChunkStatus;

public interface ITrackingCubeStatusListener {

    ChunkStatus getCubeStatus(int x, int y, int z);
}
