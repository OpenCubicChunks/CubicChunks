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
import cubicchunks.worldgen.dependency.Dependency;
import cubicchunks.worldgen.dependency.DependencyManager;
import cubicchunks.worldgen.dependency.Dependent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratorPipeline {

	private static final int TickBudget = 40; // ms. There are only 50 ms per tick

	private ServerCubeCache cubes;
	private List<GeneratorStage> stages;
	private Map<String, GeneratorStage> stageMap;
	
	private DependencyManager dependencyManager;
	

	public GeneratorPipeline(ServerCubeCache cubes) {
		this.cubes = cubes;
		this.stages = new ArrayList<GeneratorStage>();
		this.stageMap = new HashMap<String, GeneratorStage>();
		this.stageMap.put(GeneratorStage.LIVE.getName(), GeneratorStage.LIVE);
		this.dependencyManager = new DependencyManager(cubes, this);
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
					throw new Error("Generator pipline configured incorrectly! Stage " + stage.getName() +
							" is null! Fix your WorldServerContext constructor!");
				}
			}
		}
	}

	public void generate(Cube cube) {
		this.generate(cube, GeneratorStage.LIVE);
	}

	public void resume(Cube cube) {
		cube.getCurrentStage().getProcessor().processor.add(cube.getAddress());
	}

	public void generate(Cube cube, GeneratorStage targetStage) {
		GeneratorStage stage = cube.getCurrentStage();
		
		// Make sure the proper target stage is set.
		if (cube.getTargetStage().precedes(targetStage)) {
			cube.setTargetStage(targetStage);
		}
		
		// If the cube has not yet reached the target stage, queue it for processing.
		if (cube.isBeforeStage(cube.getTargetStage())) {
			
			// Register dependencies.
			Dependency dependency = stage.getDependency(cube);
			if (dependency != null) {
				Dependent dependent = new Dependent(cube, dependency);
				this.dependencyManager.register(dependent);

				if (dependent.isSatisfied()) {
					stage.getProcessor().processor.add(cube.getAddress());
				}
			} else {
				stage.getProcessor().processor.add(cube.getAddress());
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
			
			/* DEBUG
			CubicChunks.log.info("Stage {} processed {} cubes in {} ms of {}/{} ms ({}%).",
				processor.processor.getName(),
				numStageProcessed,
				System.currentTimeMillis() - stageTimeStart,
				(long)(processor.share*TickBudget),
				TickBudget,
				processor.share*100
			);
			*/

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
				
				// Advance the cube's stage.
				Cube cube = this.cubes.getCube(cubeX, cubeY, cubeZ);
				cube.setCurrentStage(nextStage);
				
				// Clear the cube's dependency.
				this.dependencyManager.unregister(cube);
				
				// Update cubes depending on this cube.
				this.dependencyManager.updateDependents(cube);
				
				// If the cube has not yet reached its target stage, continue.
				if (cube.isBeforeStage(cube.getTargetStage())) {
					Dependency dependency = stage.getDependency(cube);
					if (dependency != null) {
						Dependent dependent = new Dependent(cube, dependency);
						this.dependencyManager.register(dependent);

						if (dependent.isSatisfied()) {
							nextStage.getProcessor().processor.add(address);
						}
					} else {
						nextStage.getProcessor().processor.add(address);
					}
				}
			}
		} 
				
		else {
			for (long address : processor.getProcessedAddresses()) {
				int cubeX = AddressTools.getX(address);
				int cubeY = AddressTools.getY(address);
				int cubeZ = AddressTools.getZ(address);

				// Update the cube's stage.
				Cube cube = this.cubes.getCube(cubeX, cubeY, cubeZ);
				cube.setCurrentStage(GeneratorStage.LIVE);

				// Update dependent cubes.
				this.dependencyManager.updateDependents(cube);
			}
		}
	}


	public GeneratorStage getStage(String name) {
		return this.stageMap.get(name);
	}

	public GeneratorStage getFirstStage() {
		return this.stages.get(0);
	}


	public DependencyManager getDependencyManager() {
		return this.dependencyManager;
	}
}
