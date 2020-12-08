package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import net.minecraft.world.level.chunk.ChunkAccess;

public class LightSurfaceTrackerWrapper extends SurfaceTrackerWrapper {
	public LightSurfaceTrackerWrapper(ChunkAccess chunkAccess) {
		// type shouldn't matter
		super(chunkAccess, Types.WORLD_SURFACE, new LightSurfaceTrackerSection());
	}
}
