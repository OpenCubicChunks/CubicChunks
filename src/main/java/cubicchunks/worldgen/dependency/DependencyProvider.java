package cubicchunks.worldgen.dependency;

import cubicchunks.world.cube.Cube;

public interface DependencyProvider {

	public Dependency getDependency(Cube cube);

}
