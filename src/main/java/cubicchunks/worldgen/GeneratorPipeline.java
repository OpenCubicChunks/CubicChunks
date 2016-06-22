/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.worldgen;

import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.CubeCoords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.concurrency.CubeWorkerQueue;
import cubicchunks.worldgen.concurrency.GeneratorWorker;
import cubicchunks.worldgen.dependency.DependentCube;
import cubicchunks.worldgen.dependency.DependentCubeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratorPipeline {

	private static final int MAX_CUBES_PER_TICK = 500;

	private static final int MAX_DURATION_PER_TICK = 40;

	private static final int REPORT_INTERVAL = 40;


	private final ServerCubeCache cubeProvider;

	private final List<GeneratorStage> stages;

	private final Map<String, GeneratorStage> stageMap;

	private final DependentCubeManager dependentCubeManager;

	private List<CubeWorkerQueue> queues;

	// Reporting

	private int[] stageProcessed;

	private int[] stageDuration;

	// TODO: Remove

	private List<GeneratorWorker> workers;


	public GeneratorPipeline(ServerCubeCache cubeProvider) {
		this.cubeProvider = cubeProvider;
		this.stages = new ArrayList<>();
		this.stageMap = new HashMap<>();
		this.stageMap.put(GeneratorStage.LIVE.getName(), GeneratorStage.LIVE);
		this.dependentCubeManager = new DependentCubeManager(cubeProvider.getDependencyManager());
	}

	public void addStage(GeneratorStage stage, CubeProcessor processor) {
		stage.setOrdinal(this.stages.size());
		stage.setCubeProcessor(processor);
		this.stages.add(stage);
		this.stageMap.put(stage.getName(), stage);

		if (this.stages.size() > 1) {
			this.stages.get(this.stages.size() - 2).setNextStage(stage);
		}
	}

	public void checkStages() {

		this.queues = new ArrayList<>(this.stages.size());
		this.workers = new ArrayList<>(this.stages.size());

		for (GeneratorStage stage : this.stages) {
			if (!stage.isLastStage()) {
				if (stage.getCubeProcessor() == null) {
					throw new Error("Generator pipeline configured incorrectly! Stage " + stage.getName() +
							" is null! Fix your WorldServerContext constructor!");
				}
			}

			CubeWorkerQueue queue = new CubeWorkerQueue(stage.getName(), this.cubeProvider);
			GeneratorWorker worker = new GeneratorWorker(this, stage, queue);

			this.queues.add(queue);
			this.workers.add(worker);
		}

		for (GeneratorWorker worker : this.workers) {
			for (CubeWorkerQueue queue : this.queues) {
				queue.register(worker);
			}

			worker.signalPause();
			new Thread(worker).start();
		}

		this.stages.get(this.stages.size() - 1).setNextStage(GeneratorStage.LIVE);

		// Reporting
		this.stageProcessed = new int[this.stages.size()];
		this.stageDuration = new int[this.stages.size()];
	}


	public DependentCubeManager getDependentCubeManager() {
		return this.dependentCubeManager;
	}


	public void resume(Cube cube) {
		this.queues.get(cube.getCurrentStage().getOrdinal()).add(cube.getCoords());
	}

	public void generate(Cube cube) {

		// If the cube has reached its target stage, don't do anything.
		if (cube.hasReachedTargetStage()) {
			return;
		}

		// If the cube has dependencies, register it at the dependency manager and let it handle the rest.
		CubeDependency cubeDependency = cube.getCurrentStage().getCubeDependency(cube);
		if (cubeDependency != null) {
			DependentCube dependentCube = new DependentCube(this, cube, cubeDependency);
			this.dependentCubeManager.register(dependentCube);

		// Otherwise, start processing.
		} else {
			resume(cube);
		}
	}

	public void generate(Cube cube, GeneratorStage targetStage) {

		// Make sure the proper target stage is set.
		if (cube.getTargetStage().precedes(targetStage)) {
			cube.setTargetStage(targetStage);
		}

		this.generate(cube);
	}

	int totalProcessed = 0;
	int totalDuration = 0;
	int ticksSinceReport = 0;
	int totalFinished = 0;

	public int tick() {

		for (GeneratorWorker worker : this.workers) {
			worker.signalUnpause();
		}

		try {
			Thread.sleep(40);
		} catch (Exception e) {

		}

		for (GeneratorWorker worker : this.workers) {
			worker.signalPause();
		}

		for (GeneratorWorker worker : this.workers) {
			worker.waitUntilPaused();
	 }

		for (GeneratorWorker worker : this.workers) {
			worker.report();
		}

		for (CubeWorkerQueue queue : this.queues) {
			queue.report();
		}

		return 0;
	}

	public void processAll() {

		for (GeneratorWorker worker : this.workers) {
			worker.signalUnpause();
		}

		try {
			Thread.sleep(40);
		} catch (Exception e) {

		}

		for (GeneratorWorker worker : this.workers) {
			worker.signalPause();
		}

		for (GeneratorWorker worker : this.workers) {
			worker.waitUntilPaused();
		}

		// Force a report.
		this.ticksSinceReport = REPORT_INTERVAL;

	}


//	private void report() {
//		++ticksSinceReport;
//		if (ticksSinceReport >= REPORT_INTERVAL) {
//
//			for (int i = 0; i < this.queues.size(); ++i) {
//				CubicChunks.LOGGER.info(String.format("\t%15s: %3d processed (%.1f/s), %d in queue", this.stages.get(i).getName(), stageProcessed[i], ((float) stageProcessed[i] * 1000f) / stageDuration[i], this.queues.get(i).size()));
//
//				totalProcessed += stageProcessed[i];
//				stageProcessed[i] = 0;
//				totalDuration += stageDuration[i];
//				stageDuration[i] = 0;
//			}
//			CubicChunks.LOGGER.info("Total: Processed: {}/s Finished: {}/s", ((double) totalProcessed * 1000f) / totalDuration, ((double) totalFinished * 1000f) / totalDuration);
//
//			ticksSinceReport = 0;
//		}
//	}

	public GeneratorStage getStage(String name) {
		return this.stageMap.get(name);
	}

	public GeneratorStage getFirstStage() {
		return this.stages.get(0);
	}

}
