package cubicchunks.worldgen.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cubicchunks.world.cube.Cube;

public class Dependent {

	public Cube cube;

	public Dependency dependency;

	public Map<Long, Requirement> requirements;
	
	public int remaining;


	public Dependent(Cube cube, Dependency dependency) {
		this.cube = cube;
		this.dependency = dependency;
		this.requirements = new HashMap<Long, Requirement>();
		for (Requirement requirement : dependency.getRequirements(cube)) {
			this.requirements.put(requirement.getAddress(), requirement);
		}
		this.remaining = this.requirements.size();
	}

	public boolean update(DependencyManager manager, Cube requiredCube) {
		boolean noLongerRequired = this.dependency.update(manager, this, requiredCube);
		if (noLongerRequired) {
			--this.remaining;
		}
		return noLongerRequired;
	}

	public boolean isSatisfied() {
		return this.remaining == 0;
	}

	public Collection<Requirement> getRequirements() {
		return requirements.values();
	}
}
