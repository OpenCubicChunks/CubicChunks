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

import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.ServerCubeCache.LoadType;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The DependencyManager keeps track of Dependents and the cubes they require. When a Dependent is registered, the
 * DependencyManager retrieves the Dependent's Requirements. It will automatically load required Cubes and notify the
 * Dependents whenever the state of a RequiredCube changes.
 * RequiredCubes will not be generated fully, as this would cause an infinite chain of Cubes being loaded. Instead,
 * Requirements specify the GeneratorStage that they require a cube to be in and the DependencyManager in combination
 * with the ServerCubeCache and the GeneratorPipeline will process the Cube up to that stage. If multiple Dependents
 * require the same Cube, it will be processed up to the latest required stage.
 */
public class DependencyManager {

	/**
	 * The ServerCubeCache utilizing this DependencyManager.
	 */
	private ServerCubeCache cubeProvider;

	/**
	 * Contains an instance of RequiredCube for each cube which is currently required by at least one Dependent.
	 */
	private Map<CubeCoords, RequiredCube> requiredMap;

	/**
	 * Creates a new instance of DependencyManager for managing cube dependencies for the given ServerCubeCache.
	 *
	 * @param cubeProvider The ServerCubeCache using this instance.
	 */
	public DependencyManager(@Nonnull ServerCubeCache cubeProvider) {
		this.cubeProvider = cubeProvider;
		this.requiredMap = new HashMap<>();
	}

	/**
	 * Registers a given Dependent. All of the Cubes it requires will be loaded up to the GeneratorStage it needs them
	 * to be at and it will be notified whenever one of the Cubes it requires advances a stage.
	 *
	 * @param dependent The Dependent to be registered.
	 */
	public void register(@Nonnull Dependent dependent) {

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

			// If the required cube is loaded, ...
			if (requiredCube.isAvailable()) {

				// and it has reached the required target stage, notify the dependent.
				if (!requiredCube.getCube().isBeforeStage(requirement.getTargetStage())) {
					dependent.update(this, requiredCube.getCube());
				}
				// If the required cube's current stage precedes the required target stage, load it up to the required
				// target stage.
				else if (requiredCube.getCube().getCurrentStage().precedes(requirement.getTargetStage())) {
					this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
				}
			}
			// Otherwise load it.
			else {
				this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
			}
		}
	}

	/**
	 * Unregisters a given Dependent. Formerly required Cubes will no longer be protected from being unloaded unless
	 * other Dependents keep them around. The DependentCube will no longer be notified if one of its former
	 * Requirements advances a stage.
	 *
	 * @param dependent The Dependent to be unregistered.
	 */
	public void unregister(@Nonnull Dependent dependent) {

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
	 * Must be invoked whenever a Cube, has been loaded. Notifies all Dependents of said Cube.
	 *
	 * @param cube The newly loaded Cube.
	 */
	public void onLoad(@Nonnull Cube cube) {
		RequiredCube requiredCube = this.requiredMap.get(cube.getCoords());
		if (requiredCube != null) {
			requiredCube.setCube(cube);
			requiredCube.updateDependents(this);
		}
	}

	/**
	 * Checks if the Cube at a given set of coordinates is currently required to be loaded.
	 *
	 * @return True iff there exists at least one Dependent requiring the given Cube to remain loaded.
	 */
	public boolean isRequired(@Nonnull CubeCoords coords) {
		RequiredCube requiredCube = this.requiredMap.get(coords);
		return requiredCube != null && requiredCube.isRequired();
	}

	/**
	 * Must be invoked whenever a Cube advances to a new GeneratorStage. Notifies all Dependents of the Cube.
	 *
	 * @param cube The Cube for which its Dependents will be notified.
	 */
	public void updateDependents(@Nonnull Cube cube) {
		RequiredCube requiredCube = this.requiredMap.get(cube.getCoords());
		if (requiredCube != null) {
			requiredCube.updateDependents(this);
		}
	}

	/**
	 * Returns the total number of Cubes currently being required.
	 *
	 * @return The number of Cubes currently being required to be loaded.
	 */
	public int getRequiredCubeCount() {
		return this.requiredMap.size();
	}

}