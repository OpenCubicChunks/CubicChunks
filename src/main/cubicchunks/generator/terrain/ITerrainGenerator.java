package cubicchunks.generator.terrain;

import cubicchunks.world.cube.Cube;

public interface ITerrainGenerator {

	public abstract double[][][] generate(final Cube cube);

}