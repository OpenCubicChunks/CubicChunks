package cubicchunks.worldgen.dependency;

public interface Dependent {

	public Dependency getDependency();
	
	public void onSatisfaction();

}
