package cubicchunks.worldgen;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.dependency.DependentCubeManager;

public interface ICubeGenerator {

	int getQueuedCubeCount();

	GeneratorPipeline getGeneratorPipeline();

	DependentCubeManager getDependentCubeManager();

	void resumeCube(Cube cube);

	void generateCube(Cube cube);

	void generateCube(Cube cube, GeneratorStage targetStage);

	Cube generateCube(CubeCoords coords, GeneratorStage targetStage);

	Column generateColumn(int cubeX, int cubeZ);

	void removeCube(CubeCoords coords);

	void calculateAll();

	void tick();

}
