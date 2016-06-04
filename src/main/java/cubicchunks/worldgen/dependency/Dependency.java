package cubicchunks.worldgen.dependency;

import java.util.Collection;

import cubicchunks.world.cube.Cube;

public interface Dependency {

	public boolean isSatisfied();

	public boolean update(Cube cube);

	public Collection<Long> getRequirements();

	public boolean dependsOn(Cube cube);
	
}
