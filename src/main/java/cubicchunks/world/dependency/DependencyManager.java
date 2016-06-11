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

import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.ServerCubeCache.LoadType;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
 * TODO: Commenting
 */
public class DependencyManager {

	// TODO: Make private.
	public ServerCubeCache cubeProvider;

	private Map<CubeCoords, RequiredCube> requiredMap;


	public DependencyManager(ServerCubeCache cubeProvider) {
		this.cubeProvider = cubeProvider;
		this.requiredMap = new HashMap<>();
	}


	/**
	 * Registers a given Dependent. All of the cubes it requires will be loaded up to the stage it needs them to be
	 * at and it will be notified whenever one of its required cubes advances a stage.
	 *
	 * @param dependent The Dependent to be registered.
	 */
	public void register(Dependent dependent) {

		// If the dependent does not have any requirements, there is nothing left to do here.
		Collection<Requirement> requirements = dependent.getRequirements();

		// Add all of the requirements.
		for (Requirement requirement : requirements) {

			// If the required cube is required by other dependents already, retrieve its RequiredCube instance,
			// otherwise create a new one.
			RequiredCube requiredCube = this.requiredMap.get(requirement.getCoords());
			if (requiredCube == null) {

				// If the required cube is loaded already, add it to the RequiredCube instance immediately.
				Cube cube = this.cubeProvider.getCube(requirement.getCoords());
				if (cube != null) {
					requiredCube = new RequiredCube(cube);
				}
				// Otherwise create an empty instance.
				else {
					requiredCube = new RequiredCube();
				}

				this.requiredMap.put(requirement.getCoords(), requiredCube);
			}
			// Make sure the required cube's target stage is correct.
			else {
				requiredCube.setTargetStage(requirement.getTargetStage());
			}

			// Add the dependent to the required cubes list of dependents.
			requiredCube.addDependent(dependent);

			// If the required cube is loaded, notify the dependent.
			if (requiredCube.isAvailable()) {
				dependent.update(this, requiredCube.getCube());

				// Has the cube reached its target stage?
				if (!requiredCube.getCube().isBeforeStage(requiredCube.getCube().getTargetStage())) {

					// If the cube is supposed to reach a later stage, resume its generation.
					if (requiredCube.getCube().getCurrentStage().precedes(requirement.getTargetStage())) {
						this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
					}
				}
			}
			// Otherwise load it.
			else {
				this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
			}
		}
	}

	/**
	 * Unregisters a given Dependent. Formerly required cubes will no longer be protected from being unloaded unless
	 * other dependencies keep them around. The DependentCube will no longer be notified if one of its former
	 * requirements advances a stage.
	 *
	 * @param dependent The DependentCube to be unregistered.
	 */
	public void unregister(Dependent dependent) {

		// Remove all of the requirements.
		Collection<Requirement> requirements = dependent.getRequirements();
		for (Requirement requirement : requirements) {

			// Get the required cube.
			RequiredCube requiredCube = this.requiredMap.get(requirement.getCoords());

			// Remove the dependent from the required cube's list of dependents.
			requiredCube.removeDependent(dependent);

			// If this was the last cube depending on the current required cube, forget about the required cube.
			if (!requiredCube.isRequired()) {
				this.requiredMap.remove(requirement.getCoords());
			}
		}
	}

	/**
	 * Must be invoked whenever a cube, which has reached its target stage, has been loaded. Notifies all dependents of
	 * the cube.
	 *
	 * @param cube The newly loaded cube.
	 */
	public void onLoad(Cube cube) {
		RequiredCube requiredCube = this.requiredMap.get(cube.getCoords());
		if (requiredCube != null) {
			requiredCube.setCube(cube);
			requiredCube.update(this);
		}
	}

	/**
	 * Checks if the cube at a given set of coordinates is currently required to be loaded.
	 *
	 * @return True iff there exists at least one dependent requiring the given cube to remain loaded.
	 */
	public boolean isRequired(CubeCoords coords) {
		RequiredCube requiredCube = this.requiredMap.get(coords);
		return requiredCube != null && requiredCube.isRequired();
	}

	/**
	 * Must be invoked whenever a cube advances to a new stage. Notifies all dependents of the cube.
	 *
	 * @param cube The cube for which its dependents will be notified.
	 */
	public void updateDependents(Cube cube) {
		RequiredCube requiredCube = this.requiredMap.get(cube.getCoords());
		if (requiredCube != null) {
			requiredCube.update(this);
		}
	}

	/**
	 * {@link #isRequired(CubeCoords coords)}
	 *
	 * @return True iff there exists at least one dependent requiring the given cube to remain loaded.
	 */
	public boolean isRequired(Cube cube) {
		return this.isRequired(cube.getCoords());
	}

}