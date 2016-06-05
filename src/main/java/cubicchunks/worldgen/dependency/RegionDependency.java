package cubicchunks.worldgen.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cubicchunks.util.AddressTools;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;

public class RegionDependency implements Dependency {
	
	private GeneratorStage targetStage;
	
	private Set<Long> requiredCubes;
	
	private Set<Requirement> requirements;

	
	public RegionDependency(Cube cube, GeneratorStage targetStage, int radius) {
		
		this.targetStage = targetStage;
		this.requirements = new HashSet<Requirement>();
		
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();
		
		for (int x = -radius; x <= radius; ++x) {
			for (int y = -radius; y <= radius; ++y) {
				for (int z = -radius; z <= radius; ++z) {
					if (x != 0 || y != 0 || z != 0) {
						this.requirements.add(new Requirement(cubeX + x, cubeY + y, cubeZ + z, targetStage));
					}
				}
			}
		}
	}
	
	public RegionDependency(Cube cube, GeneratorStage stage, int xLow, int xHigh, int yLow, int yHigh, int zLow, int zHigh) {

		this.targetStage = stage;
		this.requirements = new HashSet<Requirement>();
		
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();
		
		for (int x = xLow; x <= xHigh; ++x) {
			for (int y = yLow; y <= yHigh; ++y) {
				for (int z = zLow; z <= zHigh; ++z) {
					if (x != 0 || y != 0 || z != 0) {
						this.requirements.add(new Requirement(cubeX + x, cubeY + y, cubeZ + z, targetStage));
					}
				}
			}
		}
	}

	@Override
	public boolean isSatisfied() {
		return this.requirements.size() == 0;
	}

	@Override
	public boolean update(DependencyManager manager, Dependent dependent, Cube requiredCube) {
		if (!dependent.cube.getCurrentStage().precedes(targetStage)) {
			this.requirements.remove(requiredCube.getAddress());
		}
		return true;
	}

	@Override
	public Collection<Requirement> getRequirements() {
		return this.requirements;
	}

	@Override
	public boolean dependsOn(Cube cube) {
		return this.requirements.contains(cube.getAddress());
	}

}
