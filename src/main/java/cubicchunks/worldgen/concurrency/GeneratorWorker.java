package cubicchunks.worldgen.concurrency;

import cubicchunks.CubicChunks;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorPipeline;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.dependency.DependentCubeManager;

public class GeneratorWorker extends CubeWorker {

	private final GeneratorPipeline generatorPipeline;

	private final DependentCubeManager dependentCubeManager;

	// TODO: Make private
	public final GeneratorStage generatorStage;

	private final CubeProcessor processor;


	public GeneratorWorker(GeneratorPipeline generatorPipeline, GeneratorStage generatorStage, CubeWorkerQueue queue) {
		super(generatorStage.getName(), queue);
		this.generatorPipeline = generatorPipeline;
		this.dependentCubeManager = generatorPipeline.getDependentCubeManager();
		this.generatorStage = generatorStage;
		this.processor = generatorStage.getCubeProcessor();
	}


	@Override
	public boolean process(Cube cube) {

		// If the cube is not in the correct stage, don't do anything to it.
		if (cube.getCurrentStage() != this.generatorStage) {
			return false;
		}

		// Process the cube.
		this.processor.calculate(cube);

		// Free the cube's requirements.
		this.dependentCubeManager.unregister(cube);

		// Advance the cube's stage.
		cube.setCurrentStage(this.generatorStage.getNextStage());

		// Update the cube's dependents.
		this.dependentCubeManager.updateDependents(cube);

		// If the cube has not yet reached its target stage, advance it to the next stage.
		if (cube.getCurrentStage() != GeneratorStage.LIVE && !cube.hasReachedTargetStage()) {
			this.generatorPipeline.generate(cube);
		}

		return true;
	}

}
