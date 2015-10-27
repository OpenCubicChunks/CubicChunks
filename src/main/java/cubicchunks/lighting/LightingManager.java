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
package cubicchunks.lighting;

import cubicchunks.CubicChunks;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.column.Column;
import net.minecraft.world.World;

import java.util.Set;

public class LightingManager {
	
	private static final int TickBudget = 10; // ms. Only 50 ms in a tick @ 20 tps
	
	private World world;

	private SkyLightUpdateProcessor skylightUpdateProcessor;
	private SkyLightCubeDiffuseProcessor skylightCubeDiffuseProcessor;

	private FirstLightProcessor firstLightProcessor;

	private SkyLightUpdateCalculator skylightUpdateCalculator;
	private SkyLightCubeDiffuseCalculator skylightCubeDiffuseCalculator;

	public LightingManager(World world, ICubeCache provider) {
		this.world = world;
		
		this.skylightCubeDiffuseProcessor = new SkyLightCubeDiffuseProcessor(this ,"Sky Light Diffuse", provider, 50);
		this.skylightUpdateProcessor = new SkyLightUpdateProcessor(this, "Sky Light Update", provider, 10);

		this.firstLightProcessor = new FirstLightProcessor("First Light", provider, 1);

		this.skylightCubeDiffuseCalculator = new SkyLightCubeDiffuseCalculator();
		this.skylightUpdateCalculator = new SkyLightUpdateCalculator();
	}

	public void columnSkylightUpdate(UpdateType type, Column column, int localX, int minY, int maxY, int localZ) {
		int blockX = Coords.localToBlock(column.getX(), localX);
		int blockZ = Coords.localToBlock(column.getZ(), localZ);
		switch(type) {
			case IMMEDIATE:
				Set<Integer> toDiffuse = skylightUpdateCalculator.calculate(column, localX, localZ, minY, maxY);
				for(int y : toDiffuse)
					skylightCubeDiffuseCalculator.calculate(column, localX, localZ, y);
				break;
			case IMMEDIATE_UPDATE_QUEUED_DIFFUSE:
				toDiffuse = skylightUpdateCalculator.calculate(column, localX, localZ, minY, maxY);
				for(int y : toDiffuse) {
					SkyLightCubeDiffuseProcessor.Entry entry = new SkyLightCubeDiffuseProcessor.Entry(blockX, blockZ, y);
					skylightCubeDiffuseProcessor.add(entry);
				}
				break;
			case QUEUED:
				skylightUpdateProcessor.add(new SkyLightUpdateProcessor.Entry(blockX, blockZ, minY, maxY));
				break;
		}
	}
	
	public void tick() {
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		//this.world.profiler.addSection("skyLightOcclusion");
		numProcessed += this.skylightCubeDiffuseProcessor.processQueueUntil(timeStop);
		//this.world.profiler.startSection("firstLight");
		numProcessed += this.firstLightProcessor.processQueueUntil(timeStop);
		//this.world.profiler.endSection();
		numProcessed += this.skylightUpdateProcessor.processQueueUntil(timeStop);
		
		// disable this spam for now
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if (numProcessed > 0) {
			CubicChunks.LOGGER.info(String.format("%s Lighting manager processed %d calculations in %d ms.", this.world.isRemote ? "CLIENT" : "SERVER", numProcessed, timeDiff));
			CubicChunks.LOGGER.info(this.skylightCubeDiffuseProcessor.getProcessingReport());
			CubicChunks.LOGGER.info(this.firstLightProcessor.getProcessingReport());
			CubicChunks.LOGGER.info(this.skylightUpdateProcessor.getProcessingReport());
		}
	}

	SkyLightUpdateCalculator getSkylightUpdateCalculator() {
		return skylightUpdateCalculator;
	}

	SkyLightCubeDiffuseCalculator getSkylightCubeDiffuseCalculator() {
		return skylightCubeDiffuseCalculator;
	}

	public SkyLightCubeDiffuseProcessor getSkylightCubeDiffuseProcessor() {
		return skylightCubeDiffuseProcessor;
	}

	public void queueFirstLightCalculation(long cubeAddress) {
		this.firstLightProcessor.add(cubeAddress);
	}

	public enum UpdateType {
		IMMEDIATE, IMMEDIATE_UPDATE_QUEUED_DIFFUSE, QUEUED
	}
}
