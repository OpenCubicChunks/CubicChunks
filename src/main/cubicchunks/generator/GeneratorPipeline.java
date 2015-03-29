/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.generator;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import cubicchunks.CubeCache;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeProcessor;
import cubicchunks.util.QueueProcessor;
import cubicchunks.world.Cube;

public class GeneratorPipeline {
	
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 40; // ms. There are only 50 ms per tick
	
	private CubeCache provider;
	private List<QueueProcessor> processors;
	
	public GeneratorPipeline(CubeCache provider) {
		this.provider = provider;
		this.processors = Lists.newArrayList();
		
		// allocate space for the stages
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				this.processors.add(null);
			}
		}
	}
	
	public void addStage(GeneratorStage stage, CubeProcessor processor) {
		this.processors.set(stage.ordinal(), processor);
	}
	
	public void checkStages() {
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				if (this.processors.get(stage.ordinal()) == null) {
					throw new Error("Generator pipline configured incorrectly! Stage " + stage.name() + " is null! Fix your CubeWorldProvider.createGeneratorPipeline()!");
				}
			}
		}
	}
	
	public void generate(Cube cube) {
		GeneratorStage stage = cube.getGeneratorStage();
		if (!stage.isLastStage()) {
			this.processors.get(stage.ordinal()).add(cube.getAddress());
		}
	}
	
	public int getNumCubes() {
		int num = 0;
		for (GeneratorStage stage : GeneratorStage.values()) {
			if (!stage.isLastStage()) {
				num += this.processors.get(stage.ordinal()).getNumInQueue();
			}
		}
		return num;
	}
	
	public int tick() {
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		for (int stage = 0; stage < this.processors.size(); stage++) {
			QueueProcessor processor = this.processors.get(stage);
			numProcessed += processor.processQueue(timeStop);
			
			// move the processed entries into the next stage of the pipeline
			int nextStage = stage + 1;
			for (long address : processor.getProcessedAddresses()) {
				// set the generator stage flag on the cube
				int cubeX = AddressTools.getX(address);
				int cubeY = AddressTools.getY(address);
				int cubeZ = AddressTools.getZ(address);
				this.provider.getCube(cubeX, cubeY, cubeZ).setGeneratorStage(GeneratorStage.values()[nextStage]);
				
				// advance the address to the next stage
				if (nextStage < this.processors.size()) {
					this.processors.get(nextStage).add(address);
				}
			}
		}
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if (numProcessed > 0) {
			log.info(String.format("Generation pipeline processed %d cubes in %d ms.", numProcessed, timeDiff));
			for (QueueProcessor processor : this.processors) {
				log.info(processor.getProcessingReport());
			}
		}
		
		return numProcessed;
	}
	
	public void generateAll() {
		while (tick() > 0)
			;
	}
}
