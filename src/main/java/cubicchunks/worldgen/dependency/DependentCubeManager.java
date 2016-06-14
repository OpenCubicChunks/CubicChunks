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
import cubicchunks.world.ICubeCache;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.DependencyManager;
import cubicchunks.worldgen.GeneratorPipeline;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

// TODO: Documentation
public class DependentCubeManager {

	/**
	 * The underlying DependencyManager used by the current world.
	 */
	private DependencyManager dependencyManager;

	/**
	 * Contains an instance of DependentCube for each cube which depends on other cubes to be loaded.
	 */
	private Map<CubeCoords, DependentCube> dependentMap;

	/**
	 * Creates a new instance of DependentCubeManager wrapping the given DependencyManager.
	 *
	 * @param dependencyManager The underlying DependencyManager.
	 */
	public DependentCubeManager(@Nonnull DependencyManager dependencyManager) {
		this.dependencyManager = dependencyManager;
		this.dependentMap = new HashMap<>();
	}

	/**
	 * Registers a given DependentCube with this DependentCubeManager and the underlying instance of DependencyManager.
	 *
	 * @see public void DependencyManager.register(Dependent dependent)
	 * @param dependentCube The DependentCube to be registered.
	 */
	public void register(@Nonnull DependentCube dependentCube) {

		// Prevent duplicate registrations.
		if (this.dependentMap.containsKey(dependentCube.getCube().getCoords())) {
			return;
		}

		// Remember the dependent.
		this.dependentMap.put(dependentCube.getCube().getCoords(), dependentCube);

		// Register the dependent at the DependencyManager.
		this.dependencyManager.register(dependentCube);

	}

	/**
	 * Unregisters a given DependentCube from this DependentCubeManager and the underlying instance of
	 * DependencyManager.
	 *
	 * @see public void DependencyManager.unregister(Dependent dependent)
	 * @param dependentCube The DependentCube to be unregistered.
	 */
	public void unregister(@Nonnull DependentCube dependentCube) {
		this.dependencyManager.unregister(dependentCube);
		this.dependentMap.remove(dependentCube.getCube().getCoords());
	}

	/**
	 * If the given cube is registered with this DependentCubeManager, unregisters it. Otherwise, does nothing.
	 *
	 * @param cube The cube to be unregistered.
	 */
	public void unregister(@Nonnull Cube cube) {
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
	public void updateDependents(@Nonnull Cube cube) {
		this.dependencyManager.updateDependents(cube);
	}

	/**
	 * Returns the total number of cubes currently depending on other cubes being loaded.
	 *
	 * @return The number of cubes currently depending on other cubes being loaded.
	 */
	public int getDependentCubeCount() {
		return this.dependentMap.size();
	}


	// TODO: Remove
	public int getRogueCubes(ICubeCache cubeCache) {
		int rogue = 0;
		for (DependentCube cube : this.dependentMap.values()) {
			if (!cubeCache.cubeExists(cube.getCube().getCoords())) {
				++rogue;
			}
		}
		return rogue;
	}
}
