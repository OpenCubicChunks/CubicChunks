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
import cubicchunks.world.dependency.DependencyManager;

import java.util.HashMap;
import java.util.Map;

public class DependentCubeManager {

	private DependencyManager dependencyManager;

	private Map<CubeCoords, DependentCube> dependentMap;


	public DependentCubeManager(DependencyManager dependencyManager) {
		this.dependencyManager = dependencyManager;
		this.dependentMap = new HashMap<>();
	}


	public void register(DependentCube dependentCube) {

		// Remember the dependent.
		this.dependentMap.put(dependentCube.getCube().getCoords(), dependentCube);

		// Register the dependent at the DependencyManager.
		this.dependencyManager.register(dependentCube);

	}

	public void unregister(DependentCube dependentCube) {
		this.dependencyManager.unregister(dependentCube);
		this.dependentMap.remove(dependentCube.getCube().getCoords());
	}

	public void unregister(Cube cube) {
		DependentCube dependentCube = this.dependentMap.get(cube.getCoords());
		if (dependentCube != null) {
			this.unregister(dependentCube);
		}
	}

	/**
	 * Must be invoked whenever a cube advances to a new stage. Notifies all dependents of the cube.
	 *
	 * @param cube The cube for which its dependents will be notified.
	 */
	public void updateDependents(Cube cube) {
		this.dependencyManager.updateDependents(cube);
	}

}
