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

package cubicchunks.world.dependency;

import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;

import java.util.Collection;
import java.util.HashSet;

public class RequiredCube {

	private Cube cube;

	private Collection<Dependent> dependents;


	public RequiredCube() {
		this.cube = null;
		this.dependents = new HashSet<>();
	}

	public RequiredCube(Cube cube) {
		this.cube = cube;
		this.dependents = new HashSet<>();
	}


	public Cube getCube() {
		return this.cube;
	}

	public void setCube(Cube cube) {
		this.cube = cube;
	}

	public boolean isAvailable() {
		return this.cube != null;
	}


	public boolean addDependent(Dependent dependent) {
		return this.dependents.add(dependent);
	}

	public boolean removeDependent(Dependent dependent) {
		return this.dependents.remove(dependent);
	}

	public boolean isRequired() {
		return this.dependents.size() > 0;
	}


	/**
	 * Notifies all of the cube's dependents about the cube's current status.
	 */
	public void update(DependencyManager manager) {
		for (Dependent dependent : this.dependents) {
			dependent.update(manager, this.cube);
		}
	}

	public void setTargetStage(GeneratorStage requiredStage) {
		if (this.cube.getTargetStage().precedes(requiredStage)) {
			this.cube.setTargetStage(requiredStage);
		}
	}

}
