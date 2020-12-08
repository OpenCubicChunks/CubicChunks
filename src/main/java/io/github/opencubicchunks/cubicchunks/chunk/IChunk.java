package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;

public interface IChunk {
	LightSurfaceTrackerWrapper getLightHeightmap();
}
