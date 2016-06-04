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
import cubicchunks.world.cube.Cube;

public class DependencyManager {

	private ServerCubeCache cubeProvider;
	
	private Map<Long, HashSet<Dependent>> requirementsToDependents;

	private Map<Long, Dependent> dependentMap;
	
	
	public DependencyManager(ServerCubeCache cubeProvider) {
		this.cubeProvider = cubeProvider;
		this.dependentMap = new HashMap<Long, Dependent>();
		this.requirementsToDependents = new HashMap<Long, HashSet<Dependent>>();
	}

	// Updates all dependents waiting for the given cube.
	public boolean update(Cube cube) {
		HashSet<Dependent> dependents = this.requirementsToDependents.get(cube.getAddress());
		if (dependents != null) {
			Iterator<Dependent> iter = dependents.iterator();
			while (iter.hasNext()) {
				if (iter.next().update()) {
					iter.remove();
				}
			}
			
			if (dependents.size() == 0) {
				this.requirementsToDependents.remove(cube.getAddress());
			}
			return true;
		}
		return false;
	}
	
	public void addRequirement(Dependent dependent, Requirement requirement) {
		dependent.requirements.put(requirement.getAddress(), requirement);
		
		// Map from the required cube's address to the dependent.
		Long requiredAddress = requirement.getAddress();
		HashSet<Dependent> dependents = requirementsToDependents.get(requiredAddress);
		
		if (dependents == null) {
			dependents = new HashSet<Dependent>();
			this.requirementsToDependents.put(requiredAddress, dependents);
		}
		dependents.add(dependent);
		
		// Load the required cube.
		CubicChunks.LOGGER.info("Loading required cube at ({}, {}, {}) to stage {}", requirement.cubeX, requirement.cubeY, requirement.cubeZ, requirement.targetStage.getName());
		this.cubeProvider.loadCube(requirement.cubeX, requirement.cubeY, requirement.cubeZ, LoadType.LOAD_OR_GENERATE, requirement.targetStage);
	}
	
	public void register(Dependent dependent) {
		for (Requirement requirement : dependent.dependency.getRequirements()) {
			addRequirement(dependent, requirement);
		}

		this.dependentMap.put(dependent.cube.getAddress(), dependent);
	}
	
	public void unregister(Cube cube) {
		Dependent dependent = this.dependentMap.get(cube.getAddress());
		if (dependent != null) {
		for (Requirement requirement : dependent.requirements.values()) {
			Set<Dependent> dependents = this.requirementsToDependents.get(requirement.getAddress());
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
