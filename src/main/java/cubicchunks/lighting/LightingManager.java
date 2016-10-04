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

import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import cubicchunks.CubicChunks;
import cubicchunks.IConfigUpdateListener;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

public class LightingManager implements IConfigUpdateListener {

	private SkyLightCubeDiffuseProcessor skylightCubeDiffuseProcessor;
	private volatile int lighingTickBudget = CubicChunks.Config.DEFAULT_LIGHTING_TICK_BUDGET;

	public LightingManager(ICubicWorld world) {
		CubicChunks.addConfigChangeListener(this);
		this.skylightCubeDiffuseProcessor = new SkyLightCubeDiffuseProcessor(world, "Sky Light Diffuse", 5);
	}

	public void columnSkylightUpdate(UpdateType type, Column column, int localX, int minY, int maxY, int localZ) {
		int blockX = Coords.localToBlock(column.getX(), localX);
		int blockZ = Coords.localToBlock(column.getZ(), localZ);
		switch (type) {
			case IMMEDIATE:
				IntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
				for (IntCursor cubeY : toDiffuse) {
					boolean success = SkyLightCubeDiffuseCalculator.calculate(column, localX, localZ, cubeY.value);
					if (!success) {
						queueDiffuseUpdate(column.getCube(cubeY.value), blockX, blockZ, minY, maxY);
					}
				}
				break;
			case QUEUED:
				toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
				for (IntCursor cubeY : toDiffuse) {
					queueDiffuseUpdate(column.getCube(cubeY.value), blockX, blockZ, minY, maxY);
				}
				break;
		}
	}

	public void tick() {
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + this.lighingTickBudget;

		this.skylightCubeDiffuseProcessor.processQueueUntil(timeStop);
	}

	public void queueDiffuseUpdate(Cube cube, int blockX, int blockZ, int minY, int maxY) {
		Cube.LightUpdateData data = cube.getLightUpdateData();
		data.queueLightUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ), minY, maxY);
		skylightCubeDiffuseProcessor.add(cube.getAddress());
	}

	@Override public void onConfigUpdate(CubicChunks.Config config) {
		this.lighingTickBudget = config.getLightingTickBudget();
	}

	public enum UpdateType {
		IMMEDIATE, QUEUED
	}
}
