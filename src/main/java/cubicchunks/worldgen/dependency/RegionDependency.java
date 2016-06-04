package cubicchunks.worldgen.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;

public class RegionDependency implements Dependency {
	
	private GeneratorStage stage;
	
	private Set<Long> requirements;

	public RegionDependency(Cube cube, GeneratorStage stage, int radius) {
		
		this.stage = stage;
		this.requirements = new HashSet<Long>();
		
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();
		
		for (int x = -radius; x <= radius; ++x) {
			for (int y = -radius; y <= radius; ++y) {
				for (int z = -radius; z <= radius; ++z) {
					requirements.add(AddressTools.getAddress(cubeX + x, cubeY + y, cubeZ + z));
				}
			}
		}
	}

	@Override
	public boolean isSatisfied() {
		return this.requirements.size() == 0;
	}

	@Override
	public boolean update(Cube cube) {
		if (!cube.getCurrentStage().precedes(stage)) {
			this.requirements.remove(cube.getAddress());
		}
		return this.requirements.size() == 0;
	}

	@Override
	public Collection<Long> getRequirements() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public boolean dependsOn(Cube cube) {
		return this.requirements.contains(cube.getAddress());
	}
	
}
