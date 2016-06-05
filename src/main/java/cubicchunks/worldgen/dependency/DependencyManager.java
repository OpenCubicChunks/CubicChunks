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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cubicchunks.CubicChunks;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.ServerCubeCache.LoadType;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorPipeline;

public class DependencyManager {

	private ServerCubeCache cubeProvider;
	
	private GeneratorPipeline generatorPipeline;
	
	private Map<Long, HashSet<Dependent>> requirementsToDependents;

	private Map<Long, Dependent> dependentMap;
	
	
	public DependencyManager(ServerCubeCache cubeProvider, GeneratorPipeline generatorPipeline) {
		this.cubeProvider = cubeProvider;
		this.generatorPipeline = generatorPipeline;
		this.dependentMap = new HashMap<Long, Dependent>();
		this.requirementsToDependents = new HashMap<Long, HashSet<Dependent>>();
	}
	
	
	public void initialize(Dependent dependent) {
		for (Requirement requirement : dependent.requirements.values()) {
			Cube cube = cubeProvider.getCube(requirement.coords);
			if (cube != null) {
				dependent.update(this, cube);
			}
		}
	}
	

	// Updates all dependents waiting for the given cube.
	public boolean updateDependents(Cube requiredCube) {
		HashSet<Dependent> dependents = this.requirementsToDependents.get(requiredCube.getAddress());
		if (dependents != null) {
			
			Iterator<Dependent> iter = dependents.iterator();
			while (iter.hasNext()) {
				Dependent dependent = iter.next();
				
				dependent.update(this, requiredCube);
				
				if (dependent.remaining == 0) {
					generatorPipeline.resume(dependent.cube);
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean isSatisfied(Requirement requirement) {
		return this.cubeProvider.cubeExists(requirement.coords);
	}
	
	/**
	 * Adds a new requirement for the given dependent.
	 * The requirement must not yet be part of the dependent's map of requirements.
	 * 
	 * @param dependent The dependent to which the requirement is to be added.
	 * @param requirement The requirement to be added to the dependent.
	 */
	public void addRequirement(Dependent dependent, Requirement requirement) {
		
		// Does the dependent already depend on the cube?
		Requirement existing = dependent.requirements.get(requirement.coords.getAddress());
		if (existing != null) {
			
			// If it does, return.
			if (existing.contains(requirement)) {
				return;
			}
			
			// Otherwise, replace the old requirement.
			dependent.requirements.get(requirement.coords.getAddress());
			
			// If the old requirement was satisfied and the new one is not, increment the remaining counter.
			Cube requiredCube = this.cubeProvider.getCube(requirement.coords);
			if (requiredCube != null && !requiredCube.getCurrentStage().precedes(existing.targetStage) && requiredCube.getCurrentStage().precedes(requirement.targetStage)) {
				++dependent.remaining;
			}
			
		// If it does not, simply add it.
		} else {
			addNewRequirement(dependent, requirement);
		}
	}
	
	private void addNewRequirement(Dependent dependent, Requirement requirement) {
		// Map from the required cube to the dependent.
		HashSet<Dependent> dependents = this.requirementsToDependents.get(requirement.coords.getAddress());			
		if (dependents == null) {
			dependents = new HashSet<Dependent>();
			requirementsToDependents.put(requirement.coords.getAddress(), dependents);
		}
		
		dependents.add(dependent);

		// Check if the cube is loaded.
		Cube requiredCube = this.cubeProvider.getCube(requirement.coords);
		
		// If the cube is loaded, update the dependent.
		if (requiredCube != null) {
			dependent.update(this, requiredCube);
		// Otherwise load it.
		} else {
			this.cubeProvider.loadCube(requirement.coords, LoadType.LOAD_OR_GENERATE, requirement.targetStage);
		}		
	}
	
	public void register(Dependent dependent) {

		// Remember the dependent.
		CubeCoords coords = new CubeCoords(dependent.cube.getX(), dependent.cube.getY(), dependent.cube.getZ());
		this.dependentMap.put(dependent.cube.getAddress(), dependent);
		
		for (Requirement requirement : dependent.getRequirements()) {
			addNewRequirement(dependent, requirement);
		}
		
		// If all requirements are met, resume.
		if (dependent.remaining == 0) {		
			generatorPipeline.resume(dependent.cube);
		}
	}
		
	public void unregister(Cube cube) {
		Dependent dependent = this.dependentMap.get(cube.getAddress());
		if (dependent != null) {
			for (Requirement requirement : dependent.requirements.values()) {
				Set<Dependent> dependents = this.requirementsToDependents.get(requirement.coords.getAddress());
				if (dependents != null) {
					dependents.remove(dependent);
					if (dependents.size() == 0) {
						this.requirementsToDependents.remove(dependents);
					}
				}
			}
		}
	}
	
	
	
	// Returns true if the cube at the given point is required by other cubes currently being generated.
	public boolean isRequired(int cubeX, int cubeY, int cubeZ) {
		return requirementsToDependents.get(AddressTools.getAddress(cubeX, cubeY, cubeZ)) != null;
	}


}
