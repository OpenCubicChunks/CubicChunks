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

import cubicchunks.util.CubeCoords;
import cubicchunks.worldgen.GeneratorStage;

import javax.annotation.Nonnull;

/**
 * Specifies the requirements of a Dependent for a Cube. The requirement includes the required stage for the Cube to be
 * at, such that the CubeProvider and its GeneratorPipeline can determine when a Cube loaded for a Dependent does not
 * require further processing.
 */
public class Requirement {

	/**
	 * The required Cube's coordinates
	 */
	private CubeCoords coords;

	/**
	 * The GeneratorStage the required Cube must be at for this Requirement to be satisfied.
	 */
	private GeneratorStage targetStage;

	private boolean satisfied;

	/**
	 * Creates a new instance of Requirement specifying that the Cube at the given location must be at the given
	 * GeneratorStage.
	 *
	 * @param coords The coordinates of the required Cube.
	 * @param targetStage The GeneratorStage the required Cube must be at.
	 */
	public Requirement(@Nonnull CubeCoords coords, @Nonnull GeneratorStage targetStage) {
		this.coords = coords;
		this.targetStage = targetStage;
	}

	/**
	 * Returns the coordinates of this instance of RequiredCube.
	 *
	 * @return The required Cube's coordinates
	 */
	@Nonnull
	public CubeCoords getCoords() {
		return coords;
	}

	/**
	 * Returns the GeneratorStage the required Cube must be at.
	 *
	 * @return The GeneratorStage the required Cube must be at.
	 */
	@Nonnull
	public GeneratorStage getTargetStage() {
		return targetStage;
	}

	/**
	 * Returns true iff this instance of Requirement and the given Requirement share the same coordinates and the
	 * given Requirement's targetStage precedes or equals this instance's targetStage. If several Dependents require
	 * the same Cube, this method is used to determine the earliest GeneratorStage at which the required Cube must be.
	 *
	 * @param other The Cube for which it has to be checked, whether this instance encompasses it.
	 * @return True iff this instance and the given Requirement share the same coordinates and the given Requirement's
	 *         targetStage precedes or equals this instance's targetStage.
	 */
	public boolean encompasses(@Nonnull Requirement other) {
		return this.coords.equals(other.coords) && !targetStage.precedes(other.targetStage);
	}

	/**
	 * Sets whether this Requirement has been satisfied. May be used by the Dependent using this instance of Requirement
	 * to keep track of which Requirements have been satisfied and those that it needs to wait for.
	 *
	 * @param satisfied True iff this Requirement is satisfied, false otherwise.
	 */
	public void setSatisfied(boolean satisfied) {
		this.satisfied = satisfied;
	}

	/**
	 * Returns true iff this Requirement was marked as being satisfied using setSatisfied, returns false otherwise.
	 *
	 * @return True iff this Requirement was marked as being satisfied, false otherwise.
	 */
	public boolean isSatisfied() {
		return this.satisfied;
	}
}
