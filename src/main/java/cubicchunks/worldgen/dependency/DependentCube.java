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

import cubicchunks.CubicChunks;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.Dependency;
import cubicchunks.world.dependency.DependencyManager;
import cubicchunks.world.dependency.Dependent;
import cubicchunks.world.dependency.Requirement;
import cubicchunks.worldgen.GeneratorPipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DependentCube implements Dependent {

	private GeneratorPipeline generatorPipeline;

	private Cube cube;

	private Dependency dependency;

	private Map<CubeCoords, Requirement> requirements;

	private int remaining;


	public DependentCube(GeneratorPipeline generatorPipeline, Cube cube, Dependency dependency) {
		this.generatorPipeline = generatorPipeline;
		this.cube = cube;
		this.dependency = dependency;
		this.requirements = new HashMap<>();

		for (Requirement requirement : dependency.getRequirements(cube)) {
			this.requirements.put(requirement.getCoords(), requirement);
		}
		this.remaining = this.requirements.size();
	}

	public Cube getCube() {
		return cube;
	}

	public Dependency getDependency() {
		return dependency;
	}


	/*
	 * Interface: DependentCube
	 */

	public Collection<Requirement> getRequirements() {
		return requirements.values();
	}

	public Requirement getRequirementFor(CubeCoords coords) {
		return this.requirements.get(coords);
	}

	public void addRequirement(Requirement requirement) {
		this.requirements.put(requirement.getCoords(), requirement);
	}

	public void update(DependencyManager manager, Cube requiredCube) {
		boolean noLongerRequired = this.dependency.update(manager, this, requiredCube);
		if (noLongerRequired) {
			--this.remaining;

			if (this.isSatisfied()) {

				for (Requirement requirement : this.requirements.values()) {
					if (!manager.cubeProvider.cubeExists(requirement.getCoords())) {
						CubicChunks.LOGGER.info("LIES!");
						System.exit(1);
					}
				}

				this.generatorPipeline.resume(cube);
			}
		}
	}

	public boolean isSatisfied() {
		return this.remaining == 0;
	}

}
