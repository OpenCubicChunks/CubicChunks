package cubicchunks.worldgen.dependency;

import java.util.HashMap;
import java.util.Map;

import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;

public class DependencyManager {

	private Map<Long, Dependency> dependencies; 

	public DependencyManager() {
		this.dependencies = new HashMap<Long, Dependency>();
	}

	// Updates all dependencies waiting for the given cube.
	public boolean update(Cube cube) {
		Dependency dependency = this.dependencies.get(cube.getAddress());
		return dependency != null && dependency.update(cube);
	}

	// Registers a given dependency, such that it is being updated whenever a required cube advances to a new stage.
	public void register(Dependency dependency) {
		for (Long requirement : dependency.getRequirements()) {
			this.dependencies.put(requirement, dependency);
		}
	}

}
