package cubicchunks.api.generators;

import cubicchunks.world.cube.Cube;

public interface ITerrainGenerator {

	public abstract double[][][] generate(final Cube cube);
}