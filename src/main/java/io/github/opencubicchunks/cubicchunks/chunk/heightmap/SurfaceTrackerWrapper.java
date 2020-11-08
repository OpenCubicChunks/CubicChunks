package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceTrackerWrapper extends Heightmap {
	private SurfaceTrackerSection surfaceTracker;

	// TODO change constructor
	public SurfaceTrackerWrapper(ChunkAccess chunkAccess, Types types) {
		super(chunkAccess, types);
	}
}
