package cubicchunks.generator.terrain;

import static cubicchunks.util.Coords.CUBE_SIZE;
import static cubicchunks.util.TerrainGeneratorUtils.getNewCubeSizedArray;
import cubicchunks.api.generators.ITerrainGenerator;
import cubicchunks.world.cube.Cube;

public class FlatTerrainGenerator implements ITerrainGenerator {

	private final double[][][] rawDensity;

	public FlatTerrainGenerator(final long seed) {
		this.rawDensity = getNewCubeSizedArray();
	}

	@Override
	public double[][][] generate(final Cube cube) {
		generateTerrainArray(cube);

		return this.rawDensity;
	}

	private void generateTerrainArray(final Cube cube) {
		for (int x = 0; x < CUBE_SIZE; x++) {
			for (int z = 0; z < CUBE_SIZE; z++) {
				for (int y = 0; y < CUBE_SIZE; y++) {
					this.rawDensity[x][y][z] = 1;
				}
			}
		}
	}
}
