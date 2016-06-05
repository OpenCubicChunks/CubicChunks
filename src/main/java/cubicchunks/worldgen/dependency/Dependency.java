package cubicchunks.worldgen.dependency;

import java.util.Collection;

import cubicchunks.world.cube.Cube;

public interface Dependency {

	public boolean isSatisfied();

	public Collection<Requirement> getRequirements();

	public boolean dependsOn(Cube cube);

	// Returns true iff the dependent no longer requires the given cube.
	public boolean update(DependencyManager manager, Dependent dependent, Cube requiredCube);

}
