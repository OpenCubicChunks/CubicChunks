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

package cubicchunks.worldgen.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;

public class RegionDependency implements Dependency {

	private GeneratorStage targetStage;

	private int xLow;
	private int xHigh;
	private int yLow;
	private int yHigh;
	private int zLow;
	private int zHigh;


	public RegionDependency(GeneratorStage targetStage, int radius) {

		this.targetStage = targetStage;

		this.xLow = -radius;
		this.xHigh = radius;
		this.yLow = -radius;
		this.yHigh = radius;
		this.zLow = -radius;
		this.zHigh = radius;
	}

	public RegionDependency(GeneratorStage stage, int xLow, int xHigh, int yLow, int yHigh, int zLow, int zHigh) {

		this.targetStage = stage;

		this.xLow = xLow;
		this.xHigh = xHigh;
		this.yLow = yLow;
		this.yHigh = yHigh;
		this.zLow = zLow;
		this.zHigh = zHigh;
	}

	public boolean update(DependencyManager manager, DependentCube dependent, Cube requiredCube) {
		return requiredCube.getCurrentStage() == targetStage;
	}

	public Collection<Requirement> getRequirements(Cube cube) {

		Set<Requirement> requirements = new HashSet<Requirement>();

		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();

		for (int x = this.xLow; x <= this.xHigh; ++x) {
			for (int y = this.yLow; y <= this.yHigh; ++y) {
				for (int z = this.zLow; z <= this.zHigh; ++z) {
					if (x != 0 || y != 0 || z != 0) {
						CubeCoords coords = new CubeCoords(cubeX + x, cubeY + y, cubeZ + z);
						requirements.add(new Requirement(coords, targetStage));
					}
				}
			}
		}

		return requirements;
	}

}
