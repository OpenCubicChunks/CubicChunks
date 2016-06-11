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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;

public class RequiredCube {

	/**
	 * The cube whose Dependents this RequiredCube manages. Null if the cube has not yet been loaded.
	 */
	private Cube cube;

	/**
	 * A Collection containing all Dependents requiring this cube to remain loaded.
	 */
	private Collection<Dependent> dependents;


	/**
	 * Creates a new instance of RequiredCube for a cube that has not yet been loaded.
	 */
	public RequiredCube() {
		this.cube = null;
		this.dependents = new HashSet<>();
	}

	/**
	 * Creates a new instance of RequiredCube for a given cube.
	 *
	 * @param cube The cube for which this instance will keep track of its Dependents.
	 */
	public RequiredCube(@Nonnull Cube cube) {
		this.cube = cube;
		this.dependents = new HashSet<>();
	}

	/**
	 * Returns the cube whose Dependents this RequiredCube manages.
	 *
	 * @return The cube whose Dependents this RequiredCube manages.
	 */
	@Nullable
	public Cube getCube() {
		return this.cube;
	}

	/**
	 * Sets the cube whose Dependents this RequiredCube manages.
	 *
	 * @param cube The cube whose Dependents this RequiredCube manages
	 */
	public void setCube(@Nonnull Cube cube) {
		this.cube = cube;
	}

	/**
	 * Sets the GeneratorStage at which this cube must be for all of its Dependents to be satisfied. If the current
	 * targetStage precedes the given stage, sets the new stage. Otherwise, does nothing.
	 *
	 * @param requiredStage The stage to be set.
	 */
	public void setTargetStage(@Nonnull GeneratorStage requiredStage) {
		if (this.cube.getTargetStage().precedes(requiredStage)) {
			this.cube.setTargetStage(requiredStage);
		}
	}

	/**
	 * Returns true iff this cube is currently loaded on the server.
	 *
	 * @return True iff this cube is currently available.
	 */
	public boolean isAvailable() {
		return this.cube != null;
	}

	/**
	 * Adds a Dependent of this RequiredCube.
	 *
	 * @param dependent The Dependent to be added.
	 * @return True iff the dependent was added.
	 */
	public boolean addDependent(@Nonnull Dependent dependent) {
		return this.dependents.add(dependent);
	}

	/**
	 * Removes a Dependent of this RequiredCube.
	 *
	 * @param dependent The Dependent to be removed.
	 * @return True iff the dependent was removed.
	 */
	public boolean removeDependent(@Nonnull Dependent dependent) {
		return this.dependents.remove(dependent);
	}

	/**
	 * Returns true iff there is at least one cube requiring this cube to be loaded.
	 * @return
	 */
	public boolean isRequired() {
		return this.dependents.size() > 0;
	}

	/**
	 * Notifies all of the cube's dependents about the cube's current status.
	 */
	public void updateDependents(@Nonnull DependencyManager manager) {
		for (Dependent dependent : this.dependents) {
			dependent.update(manager, this.cube);
		}
	}

}
