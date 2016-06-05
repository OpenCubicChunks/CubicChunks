package cubicchunks.worldgen.dependency;

import cubicchunks.util.AddressTools;
import cubicchunks.worldgen.GeneratorStage;

public class Requirement {

	public int cubeX;
	public int cubeY;
	public int cubeZ;
	public GeneratorStage targetStage;
	
	public Requirement(int cubeX, int cubeY, int cubeZ, GeneratorStage targetStage) {
		this.cubeX = cubeX;
		this.cubeY = cubeY;
		this.cubeZ = cubeZ;
		this.targetStage = targetStage;
	}
	
	public Long getAddress() {
		return AddressTools.getAddress(cubeX, cubeY, cubeZ);
	}
	
	public boolean contains(Requirement requirement) {
		return !targetStage.precedes(requirement.targetStage);
	}
}
