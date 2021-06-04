package io.github.opencubicchunks.cubicchunks.chunk;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.heightmap.ClientSurfaceTracker;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.SurfaceTrackerWrapper;
import net.minecraft.world.level.levelgen.Heightmap;

public interface IHeightmapGetter {
    @Nullable Heightmap getHeightmap(Heightmap.Types type);

    // Do not override this
    @Nullable default ClientSurfaceTracker getClientHeightmap(Heightmap.Types type) {
        Heightmap heightmap = this.getHeightmap(type);
        if (heightmap != null && !(heightmap instanceof ClientSurfaceTracker)) {
            throw new IllegalStateException("Attempted to get client heightmap on server");
        }
        return (ClientSurfaceTracker) heightmap;
    }

    // Do not override this
    @Nullable default SurfaceTrackerWrapper getServerHeightmap(Heightmap.Types type) {
        Heightmap heightmap = this.getHeightmap(type);
        if (heightmap != null && !(heightmap instanceof SurfaceTrackerWrapper)) {
            throw new IllegalStateException("Attempted to get server heightmap on client");
        }
        return (SurfaceTrackerWrapper) heightmap;
    }
}
