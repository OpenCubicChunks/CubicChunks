package cubicchunks.worldgen.generator.vanilla;

import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.dependency.Dependency;
import cubicchunks.worldgen.dependency.DependencyProvider;
import cubicchunks.worldgen.dependency.RegionDependency;

public class TestStage extends GeneratorStage implements DependencyProvider {

	public TestStage(String name) {
		super(name);
	}

	@Override
	public Dependency getDependency(Cube cube) {
		if (cube.getY() < 16) {
			return new RegionDependency(cube, this, 0, 0, 0, 1, 0, 0);
		}
		return null;
	}
	
}
