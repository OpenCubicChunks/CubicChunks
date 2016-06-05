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

	private DependencyManager manager;

	public Map<Long, Requirement> requirements;
	
	public int remaining;


	public Dependent(Cube cube, Dependency dependency) {
		this.cube = cube;
		this.dependency = dependency;
		this.requirements = new HashMap<Long, Requirement>();
		for (Requirement requirement : dependency.getRequirements()) {
			this.requirements.put(requirement.getAddress(), requirement);
		}
		this.remaining = this.requirements.size();
	}

	public void register(DependencyManager manager) {
		this.manager = manager;
	}

	public boolean update(Cube requiredCube) {
		boolean noLongerRequired = this.dependency.update(this.manager, this, requiredCube);
		if (noLongerRequired) {
			--this.remaining;
		}
		return noLongerRequired;
	}

	public boolean isSatisfied() {
		return this.remaining == 0;
	}
}
