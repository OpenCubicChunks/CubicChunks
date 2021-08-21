package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class LightSurfaceTrackerWrapper extends SurfaceTrackerWrapper {


    public LightSurfaceTrackerWrapper(ChunkAccess chunkAccess) {
        this(chunkAccess, new LightSurfaceTrackerSection());
    }

    public LightSurfaceTrackerWrapper(ChunkAccess chunkAccess, LightSurfaceTrackerSection surfaceTrackerSection) {
        // type shouldn't matter
        super(chunkAccess, Types.WORLD_SURFACE, surfaceTrackerSection);
    }





    @Override
    public boolean update(int columnLocalX, int globalY, int columnLocalZ, BlockState blockState) {
        super.update(columnLocalX, globalY, columnLocalZ, blockState);
        int relY = blockToLocal(globalY);
        // TODO how are we going to handle making sure that unloaded sections stay updated?
        if (relY == 0) {
            SurfaceTrackerSection section = surfaceTracker.getCubeNode(blockToCube(globalY - 1));
            if (section != null) {
                section.markDirty(columnLocalX, columnLocalZ);
            }
        } else if (relY == IBigCube.DIAMETER_IN_BLOCKS - 1) {
            SurfaceTrackerSection section = surfaceTracker.getCubeNode(blockToCube(globalY + 1));
            if (section != null) {
                section.markDirty(columnLocalX, columnLocalZ);
            }
        }

        // Return value is unused
        return false;
    }

    @Override public CompoundTag saveToDisk() {
        return super.saveToDisk();
    }

    public static LightSurfaceTrackerWrapper fromDiskCompound(ChunkAccess chunkAccess, CompoundTag chunkTag) {
        CompoundTag lightHeightmapTag = (CompoundTag) chunkTag.get("LightHeightmap");
        return new LightSurfaceTrackerWrapper(chunkAccess, LightSurfaceTrackerSection.fromChunkTag(lightHeightmapTag));
    }
}
