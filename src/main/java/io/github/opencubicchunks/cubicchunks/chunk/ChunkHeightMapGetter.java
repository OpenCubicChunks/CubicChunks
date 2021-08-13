package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Map;

import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;
import net.minecraft.world.level.levelgen.Heightmap;

public interface ChunkHeightMapGetter {

    Map<Heightmap.Types, Heightmap> getHeightMaps();

    void setLightHeightmap(LightSurfaceTrackerWrapper surfaceTrackerWrapper);
}
