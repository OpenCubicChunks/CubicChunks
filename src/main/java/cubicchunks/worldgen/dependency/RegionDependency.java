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

	private int xLow;
	private int xHigh;
	private int yLow;
	private int yHigh;
	private int zLow;
	private int zHigh;
	
	
	public RegionDependency(GeneratorStage targetStage, int radius) {

		this.targetStage = targetStage;
		
		this.xLow = -radius;
		this.xHigh = radius;
		this.yLow = -radius;
		this.yHigh = radius;
		this.zLow = -radius;
		this.zHigh = radius;
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
	public boolean update(DependencyManager manager, Dependent dependent, Cube requiredCube) {
		return requiredCube.getCurrentStage() == targetStage;
	}

	@Override
	public Collection<Requirement> getRequirements(Cube cube) {
		
		Set<Requirement> requirements = new HashSet<Requirement>();
		
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();
		
		for (int x = xLow; x <= xHigh; ++x) {
			for (int y = yLow; y <= yHigh; ++y) {
				for (int z = zLow; z <= zHigh; ++z) {
					if (x != 0 || y != 0 || z != 0) {
						requirements.add(new Requirement(cubeX + x, cubeY + y, cubeZ + z, targetStage));
					}
				}
			}
		}
		
		return requirements;
	}

}
