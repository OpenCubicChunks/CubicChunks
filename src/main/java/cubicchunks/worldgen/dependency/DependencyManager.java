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
	
	private Map<CubeCoords, HashSet<Dependent>> requirementsToDependents;

	private Map<CubeCoords, Dependent> dependentMap;
	
	
	public DependencyManager(ServerCubeCache cubeProvider, GeneratorPipeline generatorPipeline) {
		this.cubeProvider = cubeProvider;
		this.generatorPipeline = generatorPipeline;
		this.dependentMap = new HashMap<CubeCoords, Dependent>();
		this.requirementsToDependents = new HashMap<CubeCoords, HashSet<Dependent>>();
	}
	
	
	public void initialize(Dependent dependent) {
		for (Requirement requirement : dependent.requirements.values()) {
			Cube cube = cubeProvider.getCube(requirement.getCoords());
			if (cube != null) {
				dependent.update(this, cube);
			}
		}
	}
	

	// Updates all dependents waiting for the given cube.
	public boolean updateDependents(Cube requiredCube) {
		HashSet<Dependent> dependents = this.requirementsToDependents.get(requiredCube.getCoords());
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
		return this.cubeProvider.cubeExists(requirement.getCoords());
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
		Requirement existing = dependent.requirements.get(requirement.getCoords());
		if (existing != null) {
			
			// If it does, return.
			if (existing.contains(requirement)) {
				return;
			}
			
			// Otherwise, replace the old requirement.
			dependent.requirements.get(requirement.getCoords());
			
			// If the old requirement was satisfied and the new one is not, increment the remaining counter.
			Cube requiredCube = this.cubeProvider.getCube(requirement.getCoords());
			if (requiredCube != null && !requiredCube.getCurrentStage().precedes(existing.getTargetStage()) && requiredCube.getCurrentStage().precedes(requirement.getTargetStage())) {
				++dependent.remaining;
			}
			
		// If it does not, simply add it.
		} else {
			addNewRequirement(dependent, requirement);
		}
	}
	
	private void addNewRequirement(Dependent dependent, Requirement requirement) {
		// Map from the required cube to the dependent.
		HashSet<Dependent> dependents = this.requirementsToDependents.get(requirement.getCoords());			
		if (dependents == null) {
			dependents = new HashSet<Dependent>();
			requirementsToDependents.put(requirement.getCoords(), dependents);
		}
		
		dependents.add(dependent);

		// Check if the cube is loaded.
		Cube requiredCube = this.cubeProvider.getCube(requirement.getCoords());
		
		// If the cube is loaded, update the dependent.
		if (requiredCube != null) {
			dependent.update(this, requiredCube);
		// Otherwise load it.
		} else {
			this.cubeProvider.loadCube(requirement.getCoords(), LoadType.LOAD_OR_GENERATE, requirement.getTargetStage());
		}		
	}
	
	public void register(Dependent dependent) {

		// Remember the dependent.
		CubeCoords coords = new CubeCoords(dependent.cube.getX(), dependent.cube.getY(), dependent.cube.getZ());
		this.dependentMap.put(dependent.cube.getCoords(), dependent);
		
		for (Requirement requirement : dependent.getRequirements()) {
			addNewRequirement(dependent, requirement);
		}
		
		// If all requirements are met, resume.
		if (dependent.remaining == 0) {		
			generatorPipeline.resume(dependent.cube);
		}
	}
		
	public void unregister(Cube cube) {
		Dependent dependent = this.dependentMap.get(cube.getCoords());
		if (dependent != null) {
			for (Requirement requirement : dependent.requirements.values()) {
				Set<Dependent> dependents = this.requirementsToDependents.get(requirement.getCoords());
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
	public boolean isRequired(CubeCoords coords) {
		return requirementsToDependents.get(coords) != null;
	}


}
