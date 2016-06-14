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

import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.world.dependency.DependencyManager;
import cubicchunks.world.dependency.Dependent;
import cubicchunks.world.dependency.Requirement;
import cubicchunks.worldgen.GeneratorPipeline;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// TODO: Documentation
public class DependentCube implements Dependent {

	/**
	 * The world's GeneratorPipeline.
	 */
	private GeneratorPipeline generatorPipeline;

	/**
	 * The depending cube.
	 */
	private Cube cube;

	/**
	 * The CubeDependency defining the cube's Requirements.
	 */
	private CubeDependency cubeDependency;

	/**
	 * Contains all of this instance's cube's Requirements arranged by their cubes' coordinates.
	 */
	private Map<CubeCoords, Requirement> requirements;

	/**
	 * The number of Requirements that have not yet been fulfilled.
	 */
	private int remaining;

	/**
	 * Creates a new instance of DependentCube.
	 *
	 * @param generatorPipeline The world's GeneratorPipeline.
	 * @param cube The depending cube.
	 * @param cubeDependency The CubeDependency defining the cube's Requirements.
	 */
	public DependentCube(@Nonnull GeneratorPipeline generatorPipeline, @Nonnull Cube cube, @Nonnull CubeDependency cubeDependency) {
		this.generatorPipeline = generatorPipeline;
		this.cube = cube;
		this.cubeDependency = cubeDependency;
		this.requirements = new HashMap<>();

		for (Requirement requirement : cubeDependency.getRequirements(cube)) {
			this.requirements.put(requirement.getCoords(), requirement);
		}
		this.remaining = this.requirements.size();
	}

	/**
	 * Returns the cube whose Requirements this DependentCube manages.
	 *
	 * @return The cube whose Requirements this DependentCube manages.
	 */
	@Nonnull
	public Cube getCube() {
		return cube;
	}

	/**
	 * Returns the CubeDependency defining this cube's Requirements.
	 *
	 * @return The CubeDependency defining this cube's Requirements.
	 */
	@Nonnull
	public CubeDependency getCubeDependency() {
		return cubeDependency;
	}


	/*
	 * Interface: Dependent
	 */

	public Collection<Requirement> getRequirements() {
		return requirements.values();
	}

	public Requirement getRequirementFor(CubeCoords coords) {
		return this.requirements.get(coords);
	}

	public void update(DependencyManager manager, Cube requiredCube) {
		boolean satisfied = this.cubeDependency.isSatisfied(manager, this, requiredCube);
		if (satisfied && !this.requirements.get(requiredCube.getCoords()).isSatisfied()) {
			this.requirements.get(requiredCube.getCoords()).setSatisfied(true);
			--this.remaining;

			if (this.isSatisfied()) {
				this.generatorPipeline.resume(cube);
			}
		}
	}

	public boolean isSatisfied() {
		return this.remaining == 0;
	}

}
