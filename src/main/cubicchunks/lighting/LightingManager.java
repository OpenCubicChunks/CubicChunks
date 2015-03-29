/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cubicchunks.util.Bits;
import cubicchunks.world.Column;
import cubicchunks.world.CubeCache;

public class LightingManager {
	
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 40; // ms. Only 50 ms in a tick
	
	private World world;
	private SkyLightOcclusionProcessor skyLightOcclusionProcessor;
	private FirstLightProcessor firstLightProcessor;
	private DiffuseLightingCalculator diffuseLightingCalculator;
	private SkyLightUpdateCalculator skyLightUpdateCalculator;
	
	public LightingManager(World world, CubeCache provider) {
		this.world = world;
		
		this.skyLightOcclusionProcessor = new SkyLightOcclusionProcessor("Sky Light Occlusion", provider, 50);
		this.firstLightProcessor = new FirstLightProcessor("First Light", provider, 10);
		this.diffuseLightingCalculator = new DiffuseLightingCalculator();
		this.skyLightUpdateCalculator = new SkyLightUpdateCalculator();
	}
	
	public void queueSkyLightOcclusionCalculation(int blockX, int blockZ) {
		long blockColumnAddress = Bits.packSignedToLong(blockX, 26, 0) | Bits.packSignedToLong(blockZ, 26, 26);
		this.skyLightOcclusionProcessor.add(blockColumnAddress);
	}
	
	public void queueFirstLightCalculation(long cubeAddress) {
		this.firstLightProcessor.add(cubeAddress);
	}
	
	public boolean computeDiffuseLighting(BlockPos pos, LightType lightType) {
		return this.diffuseLightingCalculator.calculate(this.world, pos, lightType);
	}
	
	public void computeSkyLightUpdate(Column column, int localX, int localZ, int oldMaxBlockY, int newMaxBlockY) {
		this.skyLightUpdateCalculator.calculate(column, localX, localZ, oldMaxBlockY, newMaxBlockY);
	}
	
	public void tick() {
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		numProcessed += this.skyLightOcclusionProcessor.processQueue(timeStop);
		numProcessed += this.firstLightProcessor.processQueue(timeStop);
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if (numProcessed > 0) {
			log.info(String.format("%s Lighting manager processed %d calculations in %d ms.", this.world.isClient ? "CLIENT" : "SERVER", numProcessed, timeDiff));
			log.info(this.skyLightOcclusionProcessor.getProcessingReport());
			log.info(this.firstLightProcessor.getProcessingReport());
		}
	}
}
