package cubicchunks.worldgen.dependency;

import java.util.Collection;

import cubicchunks.world.cube.Cube;

public interface Dependency {

	/**
	 * This collection specifies which cubes must be loaded for a given cube's requirements to be satisfied.
	 * 
	 * @return A collection of Requirements specifying the given cube's requirements.
	 */
	public Collection<Requirement> getRequirements(Cube cube);

	/**
	 * Called when the given cube is loaded or entered the next generation stage.
	 * 
	 * @param manager The DependencyManager used by the server.
	 * @param dependent The dependent for which the update is called.
	 * @param requiredCube The updated cube.
	 * @return True iff the dependent no longer requires the given cube.
	 */
	public boolean update(DependencyManager manager, Dependent dependent, Cube requiredCube);

}
