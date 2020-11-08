package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceTrackerWrapper extends Heightmap {
	private final SurfaceTrackerSection surfaceTracker;
	// TODO how do we deal with having multiple columns per SurfaceTracker?
//	private final ChunkAccess chunk;

	// TODO use mixin sorcery to nullify the private fields of superclass
	public SurfaceTrackerWrapper(ChunkAccess chunkAccess, Types types) {
		super(chunkAccess, types);
//		this.chunk = chunkAccess;
		this.surfaceTracker = new SurfaceTrackerSection(types);
	}

	@Override
	public boolean update(int x, int y, int z, BlockState blockState) {
		// TODO do we need to do anything else here?
		surfaceTracker.markDirty(x, z);
		// TODO not sure if this is safe to do or if things depend on the result
		return false;
	}

	@Override
	public int getFirstAvailable(int x, int z) {
		return surfaceTracker.getHeight(x, z);
	}

	// TODO not sure what to do about these methods
	@Override
	public void setRawData(long[] ls) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long[] getRawData() {
		throw new UnsupportedOperationException();
	}
}
