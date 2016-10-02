package cubicchunks.server.chunkio.async;

import cubicchunks.CubicChunks;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.world.column.Column;

import java.io.IOException;

/**
 * Async loading of columns. Roughly equivalent to Forge's ChunkIOProvider
 */
class AsyncColumnIOProvider extends AsyncIOProvider<Column> {
	private final CubeIO loader;
	private Column column; // The target
	private final QueuedColumn colInfo;

	AsyncColumnIOProvider(QueuedColumn colInfo, CubeIO loader) {
		CubicChunks.LOGGER.debug("Load request: Column at " + colInfo.x + ", " + colInfo.z);
		this.loader = loader;
		this.colInfo = colInfo;
	}

	@Override void runSynchronousPart() {
		if (column != null) {
			column.setLastSaveTime(this.colInfo.world.getTotalWorldTime());
		}

		// We run the callback even if nothing was loaded, to notify waiting processes that the load failed
		runCallbacks();
	}

	@Override Column get() {
		return column;
	}

	@Override public void run() {
		synchronized (this) {
			try {
				this.column = this.loader.loadColumn(this.colInfo.x, this.colInfo.z);
				if (column == null) {
					CubicChunks.LOGGER.error("Async column load failed (Column does not exist in {} @ ({}, {})",
							this.colInfo.world, this.colInfo.x, this.colInfo.z);
				}
			} catch (IOException e) {
				CubicChunks.LOGGER.error("Could not load column in {} @ ({}, {})", this.colInfo.world, this.colInfo.x, this.colInfo.z, e);
			}

			this.finished = true;
			CubicChunks.LOGGER.debug("Load finished: Column at " + colInfo.x + ", " + colInfo.z);
			this.notifyAll();
		}
	}
}
