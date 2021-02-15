package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.heightmap.ClientLightSurfaceTracker;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;
import net.minecraft.world.level.levelgen.Heightmap;

public interface LightHeightmapGetter {
    Heightmap getLightHeightmap();
    default ClientLightSurfaceTracker getClientLightHeightmap() {
        Heightmap lightHeightmap = this.getLightHeightmap();
        if (!(lightHeightmap instanceof ClientLightSurfaceTracker)) {
            throw new Error("Kurst is dumb");
        }
        return (ClientLightSurfaceTracker) lightHeightmap;
    }

    default LightSurfaceTrackerWrapper getServerLightHeightmap() {
        Heightmap lightHeightmap = this.getLightHeightmap();
        if (!(lightHeightmap instanceof LightSurfaceTrackerWrapper)) {
        	System.out.println(lightHeightmap);
            throw new Error("Kurst is dumb");
        }
        return (LightSurfaceTrackerWrapper) lightHeightmap;
    }
}
