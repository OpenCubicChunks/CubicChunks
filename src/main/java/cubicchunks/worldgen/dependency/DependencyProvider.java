package cubicchunks.worldgen.dependency;

import cubicchunks.world.cube.Cube;

public interface DependencyProvider {

	/**
	 * Given a cube, may return a Dependency for determining the cube's requirements.
	 * 
	 * @param cube The cube for which the Dependency shall provide requirements.
	 * @return A Dependency providing a list of Requirements for the given cube or null.
	 */
	public Dependency getDependency(Cube cube);

}
