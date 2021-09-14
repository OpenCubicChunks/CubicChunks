package io.github.opencubicchunks.cubicchunks.world.level.chunk;

import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.ClientLightSurfaceTracker;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.LightSurfaceTrackerWrapper;
import net.minecraft.world.level.levelgen.Heightmap;

public interface LightHeightmapGetter {
    Heightmap getLightHeightmap();

    // Do not override this
    default ClientLightSurfaceTracker getClientLightHeightmap() {
        Heightmap lightHeightmap = this.getLightHeightmap();
        if (!(lightHeightmap instanceof ClientLightSurfaceTracker)) {
            throw new IllegalStateException("Attempted to get client light heightmap on server");
        }
        return (ClientLightSurfaceTracker) lightHeightmap;
    }

    // Do not override this
    default LightSurfaceTrackerWrapper getServerLightHeightmap() {
        Heightmap lightHeightmap = this.getLightHeightmap();
        if (!(lightHeightmap instanceof LightSurfaceTrackerWrapper)) {
            throw new IllegalStateException("Attempted to get server light heightmap on client");
        }
        return (LightSurfaceTrackerWrapper) lightHeightmap;
    }
}
