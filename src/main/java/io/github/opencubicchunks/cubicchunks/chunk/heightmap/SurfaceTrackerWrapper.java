package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.HeightmapAccess;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceTrackerWrapper extends Heightmap {
    protected final SurfaceTrackerSection surfaceTracker;
    protected final int dx;
    protected final int dz;

    public SurfaceTrackerWrapper(ChunkAccess chunkAccess, Types types) {
        super(chunkAccess, types);
        ((HeightmapAccess) this).setIsOpaque(null);
        this.surfaceTracker = new SurfaceTrackerSection(types);
        this.dx = sectionToMinBlock(chunkAccess.getPos().x);
        this.dz = sectionToMinBlock(chunkAccess.getPos().z);
    }

    protected SurfaceTrackerWrapper(ChunkAccess chunkAccess, Types types, SurfaceTrackerSection root) {
        super(chunkAccess, types);
        ((HeightmapAccess) this).setIsOpaque(null);
        this.surfaceTracker = root;
        this.dx = sectionToMinBlock(chunkAccess.getPos().x);
        this.dz = sectionToMinBlock(chunkAccess.getPos().z);
    }

    @Override
    public boolean update(int x, int y, int z, BlockState blockState) {
//        // TODO do we need to do anything else here?
//        surfaceTracker.getCubeNode(blockToCube(y)).markDirty(x + dx, z + dz);
//        // TODO not sure if this is safe to do or if things depend on the result
//        return false;
		// FIXME soft fail for debugging
		SurfaceTrackerSection node = surfaceTracker.getCubeNode(blockToCube(y));
		if (node == null) {
			System.out.println("warning: null node in surface tracker " + this);
			return false;
		}
        node.markDirty(x + dx, z + dz);
        return false;
    }

    @Override
    public int getFirstAvailable(int x, int z) {
        return surfaceTracker.getHeight(x + dx, z + dz) + 1;
    }

    // TODO not sure what to do about these methods
    @Override
    public void setRawData(long[] ls) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] getRawData() {
        BitStorage data = ((HeightmapAccess) this).getData();
        surfaceTracker.writeData(dx, dz, data, ((HeightmapAccess) this).getChunk().getMinBuildHeight());
        return data.getRaw();
    }

    public void loadCube(IBigCube cube) {
        // TODO loading should only cause marking as dirty if not loading from save file
        this.surfaceTracker.loadCube(blockToCubeLocalSection(dx), blockToCubeLocalSection(dz), cube, true);
    }

    public void unloadCube(IBigCube cube) {
        this.surfaceTracker.getCubeNode(cube.getCubePos().getY()).unloadCube(cube);
    }
}
