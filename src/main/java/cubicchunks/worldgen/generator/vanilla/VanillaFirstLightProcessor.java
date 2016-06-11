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
package cubicchunks.worldgen.generator.vanilla;

import cubicchunks.CubicChunks;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;

import java.util.Collections;
import java.util.Set;

public class VanillaFirstLightProcessor extends CubeProcessor {
	private final FirstLightProcessor actualProcessor;
	private GeneratorStage nextStage;

	public VanillaFirstLightProcessor(GeneratorStage lightingStage, GeneratorStage nextStage, ServerCubeCache cache, int batchSize) {
		super("Lighting", cache, batchSize);
		this.nextStage = nextStage;
		this.actualProcessor = new FirstLightProcessor(lightingStage, "Lighting", cache, batchSize);
	}

	@Override public Set<Cube> calculate(Cube cube) {
		if (cube.getY() < 0 || cube.getY() >= 16) {
			return this.actualProcessor.calculate(cube);
		}
		Set<Cube> updated = this.actualProcessor.calculate(cube);
		if (!updated.contains(cube)) {
			return Collections.emptySet();
		}
		for (int cubeY = 0; cubeY < 16; cubeY++) {
			//it should never fail, but in case it does - just ignore the error
			//and continue with wrong light values
			Cube toUpdate = cube.getColumn().getCube(cubeY);
			if (toUpdate == null) {
				CubicChunks.LOGGER.error(
						"Generating lighting for vanilla chunk (" + cube.getX() + ", " + cube.getZ() +
								"), cube at " + cubeY + " doesn't exist. Skipping update.");
				continue;
			}
			Set<Cube> currentUpdated = this.actualProcessor.calculate(toUpdate);
			if (!currentUpdated.contains(toUpdate)) {
				CubicChunks.LOGGER.error(
						"Generating lighting for vanilla chunk (" + cube.getX() + ", " + cube.getZ() +
								"), cube at " + cubeY + " - failed to update lighting.");
			}
			toUpdate.setCurrentStage(nextStage);
			updated.add(toUpdate);
		}
		return updated;
	}
}
