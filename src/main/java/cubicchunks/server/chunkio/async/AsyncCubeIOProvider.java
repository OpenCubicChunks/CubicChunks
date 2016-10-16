package cubicchunks.server.chunkio.async;

import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Async loading of cubes
 */
class AsyncCubeIOProvider extends AsyncIOProvider<Cube> {
	private final QueuedCube cubeInfo;
	private final CubeIO loader;
	private ServerCubeCache chunkCache;

	private Column column;
	private Cube cube;

	AsyncCubeIOProvider(@Nonnull QueuedCube cube, @Nonnull Column column, @Nonnull ServerCubeCache cache, @Nonnull CubeIO loader) {
		this.cubeInfo = cube;
		this.column = column;
		this.chunkCache = cache;
		this.loader = loader;
	}

	@Override
	public synchronized void run() // async stuff
	{
		try {
			// Make sure we don't load a cube from disk that has already been synchronously loaded
			cube = chunkCache.getLoadedCube(cubeInfo.x, cubeInfo.y, cubeInfo.z);

			if (cube == null) {
				cube = this.loader.loadCubeAndAddToColumn(column, this.cubeInfo.y);
			}

			if (cube == null) {
				CubicChunks.LOGGER.error("Async cube load failed (Cube does not exist in {} @ ({}, {}, {})",
						this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z);

			}
		} catch (IOException e) {
			CubicChunks.LOGGER.error("Could not load cube in {} @ ({}, {}, {})", this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z, e);
		} finally {
			this.finished = true;
			this.notifyAll();
		}
	}

	// sync stuff
	@Override
	public void runSynchronousPart() {

		// TODO: Load Entities - done in CubeIO
		// TODO: Check functionality we don't offer:

		// Done in Column:
		// this.cube.setLastSaveTime(provider.worldObj.getTotalWorldTime());

		// TBD:
		// this.provider.cubeGenerator.recreateStructures(this.cube, this.cubeInfo.x, this.cubeInfo.z);

		// No clue
		/*
		provider.id2CubeMap.put(CubePos.cubeXZ2Int(this.cubeInfo.x, this.cubeInfo.z), this.cube);
		this.cube.onCubeLoad();
		this.cube.populateCube(provider, provider.cubeGenerator);
		*/

		this.runCallbacks();
	}

	@Override
	public Cube get() {
		return cube;
	}
}
