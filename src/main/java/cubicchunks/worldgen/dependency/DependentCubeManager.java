package cubicchunks.worldgen.dependency;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.DependencyManager;

import java.util.HashMap;
import java.util.Map;

public class DependentCubeManager {

	private DependencyManager dependencyManager;

	private Map<CubeCoords, DependentCube> dependentMap;


	public DependentCubeManager(DependencyManager dependencyManager) {
		this.dependencyManager = dependencyManager;
		this.dependentMap = new HashMap<>();
	}


	public void register(DependentCube dependentCube) {

		// Remember the dependent.
		this.dependentMap.put(dependentCube.getCube().getCoords(), dependentCube);

		// Register the dependent at the DependencyManager.
		this.dependencyManager.register(dependentCube);

	}

	public void unregister(DependentCube dependentCube) {
		this.dependencyManager.unregister(dependentCube);
		this.dependentMap.remove(dependentCube.getCube().getCoords());
	}

	public void unregister(Cube cube) {
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
	public void updateDependents(Cube cube) {
		this.dependencyManager.updateDependents(cube);
	}

}
