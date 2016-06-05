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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Dependent {

	private Cube cube;

	private Dependency dependency;

	private Map<CubeCoords, Requirement> requirements;
	
	int remaining;


	public Dependent(Cube cube, Dependency dependency) {
		this.cube = cube;
		this.dependency = dependency;
		this.requirements = new HashMap<CubeCoords, Requirement>();
		for (Requirement requirement : dependency.getRequirements(cube)) {
			this.requirements.put(requirement.getCoords(), requirement);
		}
		this.remaining = this.requirements.size();
	}

	public boolean update(DependencyManager manager, Cube requiredCube) {
		boolean noLongerRequired = this.dependency.update(manager, this, requiredCube);
		if (noLongerRequired) {
			--this.remaining;
		}
		return noLongerRequired;
	}

	public boolean isSatisfied() {
		return this.remaining == 0;
	}

	public Collection<Requirement> getRequirements() {
		return requirements.values();
	}

	public Cube getCube() {
		return cube;
	}

	public Requirement getRequirementFor(CubeCoords coords) {
		return this.requirements.get(coords);
	}

	void putRequirement(Requirement requirement) {
		this.requirements.put(requirement.getCoords(), requirement);
	}
}
