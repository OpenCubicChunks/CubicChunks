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
package cubicchunks.generator;

import com.google.common.collect.Lists;
import cubicchunks.CubicChunks;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Progress;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.util.processor.QueueProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;

import java.util.List;

public class GeneratorPipeline {
	
	private static final int TickBudget = 40; // ms. There are only 50 ms per tick
	
	private static class StageProcessor {
		
		public QueueProcessor<Long> processor;
		public float share;
		
		public StageProcessor(QueueProcessor<Long> processor) {
			this.processor = processor;
			this.share = 0f;
		}
	}
	
	private ICubeCache cubes;
	private List<StageProcessor> processors;
	
	public GeneratorPipeline(ICubeCache cubes) {
		this.cubes = cubes;
		this.processors = Lists.newArrayList();
		
		// allocate space for the stages
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				this.processors.add(null);
			}
		}
	}
	
	public void addStage(GeneratorStage stage, CubeProcessor processor) {
		this.processors.set(stage.ordinal(), new StageProcessor(processor));
	}
	
	public void checkStages() {
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				if (this.processors.get(stage.ordinal()) == null) {
					throw new Error("Generator pipline configured incorrectly! Stage " + stage.name() + " is null! Fix your WorldServerContext constructor!");
				}
			}
		}
	}
	
	public void generate(Cube cube) {
		GeneratorStage stage = cube.getGeneratorStage();
		if (!stage.isLastStage()) {
			this.processors.get(stage.ordinal()).processor.add(cube.getAddress());
		}
	}
	
	public int getNumCubes() {
		int num = 0;
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				num += this.processors.get(stage.ordinal()).processor.getNumInQueue();
			}
		}
		return num;
	}
	
	public int tick() {
		long timeStart = System.currentTimeMillis();
		
		// allocate time to each stage depending on busy it is
		final int sizeCap = 500;
		int numCubes = 0;
		for (StageProcessor processor : this.processors) {
			numCubes += Math.min(sizeCap, processor.processor.getNumInQueue());
		}
		for (StageProcessor processor : this.processors) {
			if (numCubes <= 0) {
				processor.share = 0;
			} else {
				int size = Math.min(sizeCap, processor.processor.getNumInQueue());
				processor.share = (float)size/(float)numCubes;
			}
		}
		
		// process the queues
		int numProcessed = 0;
		for (int stage = 0; stage < this.processors.size(); stage++) {
			
			// process this stage according to its share
			StageProcessor processor = this.processors.get(stage);
			if (processor.share <= 0) {
				continue;
			}
			
			int numMsToProcess = (int)(Math.ceil(processor.share*TickBudget));
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
			for (StageProcessor processor : this.processors) {
				CubicChunks.LOGGER.debug(processor.processor.getProcessingReport());
			}
		}
		
		return numProcessed;
	}
	
	public void generateAll() {
		for (int stage = 0; stage < this.processors.size(); stage++) {
			
			QueueProcessor processor = this.processors.get(stage).processor;
			
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
	
	private void advanceCubes(QueueProcessor<Long> processor, int stage) {
		
		// move the processed entries into the next stage of the pipeline
		int nextStage = stage + 1;
		for (long address : processor.getProcessedAddresses()) {
			
			// set the generator stage flag on the cube
			int cubeX = AddressTools.getX(address);
			int cubeY = AddressTools.getY(address);
			int cubeZ = AddressTools.getZ(address);
			this.cubes.getCube(cubeX, cubeY, cubeZ).setGeneratorStage(GeneratorStage.values()[nextStage]);
			
			// advance the address to the next stage
			if (nextStage < this.processors.size()) {
				this.processors.get(nextStage).processor.add(address);
			}
		}
	}
}
