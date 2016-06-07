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

import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.ServerCubeCache.LoadType;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorPipeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DependencyManager {

	private ServerCubeCache cubeProvider;
	
	private GeneratorPipeline generatorPipeline;
	
	private Map<CubeCoords, HashSet<DependentCube>> requirementsToDependents;

	private Map<CubeCoords, DependentCube> dependentMap;
	
	
	public DependencyManager(ServerCubeCache cubeProvider, GeneratorPipeline generatorPipeline) {
		this.cubeProvider = cubeProvider;
		this.generatorPipeline = generatorPipeline;
		this.dependentMap = new HashMap<>();
		this.requirementsToDependents = new HashMap<>();
	}
	
	
	// Updates all dependents waiting for the given cube.
	public boolean updateDependents(Cube requiredCube) {
		
		HashSet<DependentCube> dependentCubes = this.requirementsToDependents.get(requiredCube.getCoords());
		if (dependentCubes != null) {
			for (DependentCube dependentCube : dependentCubes) {
				dependentCube.update(this, requiredCube);
				if (dependentCube.isSatisfied()) {
					generatorPipeline.resume(dependentCube.getCube());
				}
			}
			return true;
		}
		return false;
	}

	private void addNewRequirement(DependentCube dependentCube, Requirement requirement) {
		// Map from the required cube to the dependentCube.
		HashSet<DependentCube> dependentCubes = this.requirementsToDependents.get(requirement.getCoords());
		if (dependentCubes == null) {
			dependentCubes = new HashSet<>();
			requirementsToDependents.put(requirement.getCoords(), dependentCubes);
		}
		
		dependentCubes.add(dependentCube);

		// Check if the cube is loaded.
		Cube requiredCube = this.cubeProvider.getCube(requirement.getCoords());
		
		// If the cube is loaded, update the dependentCube.
		if (requiredCube != null) {
			dependentCube.update(this, requiredCube);
		// Otherwise load it.
		} else {
			this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
		}		
	}
	
	public void register(DependentCube dependentCube) {

		// Remember the dependentCube.
		CubeCoords coords = dependentCube.getCube().getCoords();
		this.dependentMap.put(coords, dependentCube);

		for (Requirement requirement : dependentCube.getRequirements()) {
			addNewRequirement(dependentCube, requirement);
		}
		
		// If all requirements are met, resume.
		if (dependentCube.isSatisfied()) {
			generatorPipeline.resume(dependentCube.getCube());
		}
	}
		
	public void unregister(Cube cube) {
		DependentCube dependentCube = this.dependentMap.get(cube.getCoords());
		if (dependentCube != null) {
			for (Requirement requirement : dependentCube.getRequirements()) {
				Set<DependentCube> dependentCubes = this.requirementsToDependents.get(requirement.getCoords());
				if (dependentCubes != null) {
					dependentCubes.remove(dependentCube);
					if (dependentCubes.size() == 0) {
						this.requirementsToDependents.remove(requirement.getCoords());
					}
				}
			}
		}
	}
	
	
	
	// Returns true if the cube at the given point is required by other cubes currently being generated.
	public boolean isRequired(CubeCoords coords) {
		return requirementsToDependents.get(coords) != null;
	}


}
