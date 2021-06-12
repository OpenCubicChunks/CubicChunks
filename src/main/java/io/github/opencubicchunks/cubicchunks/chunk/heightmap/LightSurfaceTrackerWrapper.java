package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class LightSurfaceTrackerWrapper extends SurfaceTrackerWrapper {
    public LightSurfaceTrackerWrapper(ChunkAccess chunkAccess) {
        // type shouldn't matter
        super(chunkAccess, Types.WORLD_SURFACE, new LightSurfaceTrackerSection());
    }

    @Override
    public boolean update(int x, int y, int z, BlockState blockState) {
        super.update(x, y, z, blockState);
        int relY = blockToLocal(y);
        // TODO how are we going to handle making sure that unloaded sections stay updated?
        if (relY == 0) {
            SurfaceTrackerSection section = surfaceTracker.getCubeNode(blockToCube(y - 1));
            if (section != null) {
                section.markDirty(x, z);
            }
        } else if (relY == IBigCube.DIAMETER_IN_BLOCKS - 1) {
            SurfaceTrackerSection section = surfaceTracker.getCubeNode(blockToCube(y + 1));
            if (section != null) {
                section.markDirty(x, z);
            }
        }

        // Return value is unused
        return false;
    }
}
