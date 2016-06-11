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
import cubicchunks.world.cube.Cube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface Dependent {

	/**
	 * Provides a collection containing the coordinates of all of this Dependent's required cubes and the
	 * GeneratorStages it requires those cubes to be at, in form of Requirements.
	 * @see Requirement
	 *
	 * @return A collection containing all of the Dependent's Requirements.
	 */
	@Nonnull
	Collection<Requirement> getRequirements();

	/**
	 * Provides a Requirement specifying the GeneratorStage at which this Dependent requires the cube at the given
	 * coordinates to be or null if this Dependent does not require it to be loaded.
	 *
	 * @param coords The coordinates for which the Requirement is to be retrieved.
	 * @return A Requirement if this Dependent requires the cube at the given coordinates to be loaded. Null otherwise.
	 */
	@Nullable
	Requirement getRequirementFor(@Nonnull CubeCoords coords);

	/**
	 * Called whenever a cube which this Dependent requires is loaded or advances a GeneratorStage, such that the
	 * Dependent can check if its requirements changed.
	 *
	 * @param manager The DependencyManager responsible for the current world.
	 * @param requiredCube The cube which has been updated.
	 */
	void update(@Nonnull DependencyManager manager, @Nonnull Cube requiredCube);

	/**
	 * Determines if all of the Dependent's requirements are fulfilled.
	 *
	 * @return True iff all of the Dependent's requirements are fulfilled.
	 */
	boolean isSatisfied();

}
