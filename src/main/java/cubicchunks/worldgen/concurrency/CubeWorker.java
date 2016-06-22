package cubicchunks.worldgen.concurrency;

import cubicchunks.CubicChunks;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;

// TODO: Add means to properly shutdown.

public abstract class CubeWorker implements Runnable {

	private final String name;

	private final CubeWorkerQueue queue;

	private CubeCoords assignment;

	// Synchronization

	private final Object pauseMutex;

	private boolean shouldRun;

	private boolean running;

	// Reporting

	private long totalProcessed;

	private long processedSinceReset;

	private long totalDuration;

	private long durationSinceReset;

	// Workload management

	// TODO: Make dynamic
	private final int batchSize = 500;



	public CubeWorker(String name, CubeWorkerQueue queue) {
		this.name = name;
		this.queue = queue;
		this.pauseMutex = new Object();
		this.shouldRun = false;
		this.running = false;
	}


	public void assign(CubeCoords coords) {
		synchronized (this) {
			this.assignment = coords;
		}
	}

	public CubeCoords getAssignment() {
		return this.assignment;
	}


	public abstract boolean process(Cube cube);

	public void run() {

		this.pause();

		while (true) {
			this.waitUntilUnpaused();

			long timeStart = System.currentTimeMillis();

			int processed = 0;
			while (this.canRun()) {
				// Get the next cube and process it.
				Cube cube = this.queue.poll(this);

				// If there is available cube, stop processing and continue in the next tick.
				if (cube == null) {
					break;
				}

				// Otherwise process it.
				if (this.process(cube)) {
					++processed;
				}
			}

			long timeDiff = System.currentTimeMillis() - timeStart;

			// Reporting
			this.totalDuration += timeDiff;
			this.durationSinceReset += timeDiff;
			this.totalProcessed += processed;
			this.processedSinceReset += processed;

			this.pause();
		}
	}


	// Synchronization

	public void signalPause() {
		synchronized (this.pauseMutex) {
			this.shouldRun = false;
		}
	}

	public void signalUnpause() {
		synchronized (this.pauseMutex) {
			this.shouldRun = true;
			this.pauseMutex.notifyAll();
		}
	}

	public boolean canRun() {
		synchronized (this.pauseMutex) {
			return !this.shouldRun;
		}
	}

	private void pause() {
		synchronized (this.pauseMutex) {
			this.running = false;
			this.pauseMutex.notifyAll();
		}

	}

	private void waitUntilUnpaused() {
		synchronized (this.pauseMutex) {
			while (!this.shouldRun) {
				try {
					this.pauseMutex.wait();
				} catch (InterruptedException e) {
					// TODO
				}
			}
			this.running = true;
		}
	}

	public void waitUntilPaused() {
		synchronized (this.pauseMutex) {
			while (this.running) {
				try {
					this.pauseMutex.wait();
				} catch (InterruptedException e) {
					// TODO
				}
			}
		}
	}

	// Reporting

	public void report() {
		CubicChunks.LOGGER.info("{}", this.name);
		CubicChunks.LOGGER.info("total: {} cubes at {} cubes/s", totalProcessed, ((double) totalProcessed) / totalDuration);
	}

}
