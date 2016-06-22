package cubicchunks.worldgen.concurrency;

import cubicchunks.CubicChunks;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CubeWorkerQueue {

	// TODO: Dynamicalize
	private static final int workerDistance = 5;


	private final String name;

	private final ICubeCache cubeCache;

	private final Queue<CubeCoords> queue;

	private final List<CubeWorker> workers;


	public CubeWorkerQueue(String name, ICubeCache cubeCache) {
		this.name = name;
		this.cubeCache = cubeCache;
		this.queue = new ConcurrentLinkedQueue<>();
		this.workers = new ArrayList<>();
	}


	public void register(CubeWorker worker) {
		this.workers.add(worker);
	}

	public void add(CubeCoords coords) {
		synchronized (this) {
			this.queue.add(coords);
		}
	}

	public Cube poll(CubeWorker pollingWorker) {
		synchronized (this.cubeCache) {
			synchronized (pollingWorker) {
				// Clear the polling worker's current assignment.
				pollingWorker.assign(null);

				// Iterate through the queue until an available cube is found.
				Iterator<CubeCoords> iter = this.queue.iterator();
				CubeCoords coords = iter.hasNext() ? iter.next() : null;

				while (coords != null) {
					if (isAvailable(coords)) {
						iter.remove();

						// Get the cube.
						Cube cube = this.cubeCache.getCube(coords);

						// If the cube exists, assign it to the worker and return it.
						if (cube != null) {
							pollingWorker.assign(coords);
							return cube;
						}
						// Otherwise continue.
					}

					coords = iter.hasNext() ? iter.next() : null;
				}

				// If no available cube was found, return null.
			}
		}

		return null;
	}

	public boolean isAvailable(CubeCoords coords) {
		for (CubeWorker worker : this.workers) {
			CubeCoords assignment = worker.getAssignment();
			if (assignment != null && (
					Math.abs(assignment.getCubeX() - coords.getCubeX()) < workerDistance ||
							Math.abs(assignment.getCubeZ() - coords.getCubeZ()) < workerDistance)) {
				return false;
			}
		}
		return true;
	}

	public void report() {
		CubicChunks.LOGGER.info("{}: {} remaining.", this.name,  this.queue.size());
	}
}
