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
import cubicchunks.util.AddressTools;
import cubicchunks.util.Progress;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.util.processor.QueueProcessor;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.Dependency;
import cubicchunks.worldgen.dependency.DependentCube;
import cubicchunks.worldgen.dependency.DependentCubeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratorPipeline {

	private static final int TickBudget = 40; // ms. There are only 50 ms per tick

	private ServerCubeCache cubeProvider;

	private List<GeneratorStage> stages;

	private Map<String, GeneratorStage> stageMap;

	private DependentCubeManager dependentCubeManager;


	public GeneratorPipeline(ServerCubeCache cubeProvider) {
		this.cubeProvider = cubeProvider;
		this.stages = new ArrayList<>();
		this.stageMap = new HashMap<>();
		this.stageMap.put(GeneratorStage.LIVE.getName(), GeneratorStage.LIVE);
		this.dependentCubeManager = new DependentCubeManager(cubeProvider.getDependencyManager());
	}

	public void addStage(GeneratorStage stage, CubeProcessor processor) {
		stage.setOrdinal(this.stages.size());
		stage.setProcessor(new StageProcessor(processor));
		this.stages.add(stage);
		this.stageMap.put(stage.getName(), stage);
	}

	public void checkStages() {
		for (GeneratorStage stage : this.stages) {
			if (!stage.isLastStage()) {
				if (stage.getProcessor() == null) {
					throw new Error("Generator pipeline configured incorrectly! Stage " + stage.getName() +
							" is null! Fix your WorldServerContext constructor!");
				}
			}
		}
	}


	public int getNumCubes() {
		int num = 0;

		for (GeneratorStage stage : this.stages) {
			if (!stage.isLastStage()) {
				num += stage.getProcessor().processor.getNumInQueue();
			}
		}

		return num;
	}

	public DependentCubeManager getDependentCubeManager() {
		return this.dependentCubeManager;
	}


	public void resume(Cube cube) {
		cube.getCurrentStage().getProcessor().processor.add(cube.getAddress());
	}

	public void generate(Cube cube) {

		// If the cube has reached its target stage, don't do anything.
		if (cube.hasReachedTargetStage()) {
			return;
		}

		// If the cube has dependencies, register it at the dependency manager and let it handle the rest.
		Dependency dependency = cube.getCurrentStage().getDependency(cube);
		if (dependency != null) {
			DependentCube dependentCube = new DependentCube(this, cube, dependency);
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

	public int tick() {

		long timeStart = System.currentTimeMillis();

		// allocate time to each stage depending on busy it is
		final int sizeCap = 500;
		int numCubes = 0;
		for (GeneratorStage stage : this.stages) {
			numCubes += Math.min(sizeCap, stage.getProcessor().processor.getNumInQueue());
		}
		for (GeneratorStage stage : this.stages) {
			if (numCubes <= 0) {
				stage.getProcessor().share = 0;
			} else {
				int size = Math.min(sizeCap, stage.getProcessor().processor.getNumInQueue());
				stage.getProcessor().share = (float) size/(float) numCubes;
			}
		}

		// process the queues
		int numProcessed = 0;
		for (GeneratorStage stage : this.stages) {

			// process this stage according to its share
			StageProcessor processor = stage.getProcessor();
			if (processor.share <= 0) {
				continue;
			}

			int numMsToProcess = (int) (Math.ceil(processor.share*TickBudget));
			long stageTimeStart = System.currentTimeMillis();
			int numStageProcessed = processor.processor.processQueueUntil(stageTimeStart + numMsToProcess);

			numProcessed += numStageProcessed;

			advanceCubes(processor.processor, stage);
		}

		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if (numProcessed > 0) {
			CubicChunks.LOGGER.debug("Processed {} cubes in {} ms.", numProcessed, timeDiff);
			for (GeneratorStage stage : this.stages) {
				CubicChunks.LOGGER.debug(stage.getProcessor().processor.getProcessingReport());
			}
		}

		return numProcessed;
	}

	public void generateAll() {
		for (GeneratorStage stage : this.stages) {
			
			QueueProcessor<Long> processor = stage.getProcessor().processor;

			CubicChunks.LOGGER.info("Stage: {}", processor.getName());

			// process all the cubes in this stage at once
			int numProcessed = 0;
			int round = 0;
			do {
				CubicChunks.LOGGER.info("\tround {} - {} cubes", ++round, processor.getNumInQueue());
				Progress progress = new Progress(processor.getNumInQueue(), 1000);
				numProcessed = processor.processQueue(progress);
				advanceCubes(processor, stage);
				CubicChunks.LOGGER.info("\t\tprocessed {}", numProcessed);
			} while (numProcessed > 0 && processor.getNumInQueue() > 0);
		}
	}

	private void advanceCubes(QueueProcessor<Long> processor, GeneratorStage stage) {

		// The last stage for all GeneratorPipelines is GeneratorStage.LIVE. If LIVE is the next stage,
		// all cubes have reached their target stage. Therefore unnecessary checks can be avoided.
		
		// move the processed entries into the next stage of the pipeline
		if (stage.getOrdinal() + 1 < this.stages.size()) {
			
			GeneratorStage nextStage = this.stages.get(stage.getOrdinal() + 1);	
			
			for (long address : processor.getProcessedAddresses()) {
				int cubeX = AddressTools.getX(address);
				int cubeY = AddressTools.getY(address);
				int cubeZ = AddressTools.getZ(address);

				Cube cube = this.cubeProvider.getCube(cubeX, cubeY, cubeZ);
				
				// Clear the cube's dependency.
				this.dependentCubeManager.unregister(cube);
				
				// Advance the cube's stage.
				cube.setCurrentStage(nextStage);

				// Update cubes depending on this cube.
				this.dependentCubeManager.updateDependents(cube);

				// If the next stage is not the cube's target stage, carry on.
				if (nextStage.precedes(cube.getTargetStage())) {
					generate(cube);
				}
			}
		} 
				
		else {
			for (long address : processor.getProcessedAddresses()) {
				int cubeX = AddressTools.getX(address);
				int cubeY = AddressTools.getY(address);
				int cubeZ = AddressTools.getZ(address);

				Cube cube = this.cubeProvider.getCube(cubeX, cubeY, cubeZ);

				// Clear the cube's dependency.
				this.dependentCubeManager.unregister(cube);

				// Update the cube's stage.
				cube.setCurrentStage(GeneratorStage.LIVE);

				// Update cubes depending on this cube.
				this.dependentCubeManager.updateDependents(cube);
			}
		}
	}


	public GeneratorStage getStage(String name) {
		return this.stageMap.get(name);
	}

	public GeneratorStage getFirstStage() {
		return this.stages.get(0);
	}

}
