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

import cubicchunks.world.cube.Cube;

public interface Dependency {

	/**
	 * This collection specifies which cubes must be loaded for a given cube's requirements to be satisfied.
	 *
	 * @return A collection of Requirements specifying the given cube's requirements.
	 */
	public Collection<Requirement> getRequirements(Cube cube);

	/**
	 * Called when the given cube is loaded or entered the next generation stage.
	 *
	 * @param manager The DependencyManager used by the server.
	 * @param dependent The dependent for which the update is called.
	 * @param requiredCube The updated cube.
	 *
	 * @return True iff the dependent no longer requires the given cube.
	 */
	public boolean update(DependencyManager manager, DependentCube dependent, Cube requiredCube);

}
