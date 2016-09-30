package cubicchunks.server.chunkio.async;

import cubicchunks.CubicChunks;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Async loading of cubes
 */
public class AsyncCubeIOProvider extends AsyncIOProvider<AsyncCubeIOProvider.LoadResult> {
	private final QueuedCube cubeInfo;
	private final CubeIO loader;

	private Column column;
	private LoadResult result;

	AsyncCubeIOProvider(QueuedCube cube, CubeIO loader) {
		this.cubeInfo = cube;
		this.loader = loader;
	}

	/**
	 * If the column is already loaded, it can be set here to skip a disk load
	 *
	 * @param column The column
	 */
	synchronized void setColumn(Column column) {
		if (this.column != null) {
			CubicChunks.LOGGER.warn("Trying to set column for async cube load, but column already present - possible race");
			return;
		}
		this.column = column;
	}

	@Override
	public synchronized void run() // async stuff
	{
		try {
			if (column != null) {
				column = this.loader.loadColumn(this.cubeInfo.x, this.cubeInfo.z);
			}
			if (column == null) {
				this.result = new LoadResult(LoadResult.Type.COLUMN_LOAD_FAILED, null, null);
				return;
			}

			Cube cube = this.loader.loadCubeAndAddToColumn(column, this.cubeInfo.y);
			if (cube == null) {
				CubicChunks.LOGGER.error("Async cube load failed (Cube does not exist in {} @ ({}, {}, {})",
						this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z);
				this.result = new LoadResult(LoadResult.Type.CUBE_LOAD_FAILED, null, column);
				return;
			}

			this.result = new LoadResult(LoadResult.Type.OK, cube, column);

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
	public LoadResult get() {
		return result;
	}

	public static class LoadResult {
		enum Type {OK, CUBE_LOAD_FAILED, COLUMN_LOAD_FAILED}

		@Nonnull public final Type type;
		@Nullable public final Cube cube;
		@Nullable public final Column column;

		LoadResult(@Nonnull Type type, @Nullable Cube cube, @Nullable Column column) {
			this.type = type;
			this.cube = cube;
			this.column = column;
		}
	}
}
