/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.generator;

import java.util.Random;

import net.minecraft.world.biome.Biome;
import cubicchunks.generator.noise.NoiseGeneratorMultiFractal;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.biome.BiomeBlockReplacer;
import cubicchunks.world.cube.Cube;

public class SurfaceProcessor extends CubeProcessor {

	private static final String PROCESSOR_NAME = "Surface";

	private Random rand;
	private NoiseGeneratorMultiFractal noiseGen;
	private double[] noise;
	private Biome[] biomes;
	private long seed;

	public SurfaceProcessor(final ICubeCache cubeCache, final int batchSize, final long seed) {
		super(PROCESSOR_NAME, cubeCache, batchSize);
		this.rand = new Random(seed);
		this.noiseGen = new NoiseGeneratorMultiFractal(this.rand, 4);
		this.noise = new double[256];
		this.biomes = null;
		this.seed = seed;
	}

	@Override
	public boolean calculate(final Cube cube) {

		// if the cube is empty, there is nothing to do. Even if neighbors don't
		// exist
		if (cube.isEmpty()) {
			return true;
		}

		if (!this.canGenerate(cube)) {
			return false;
		}

		this.biomes = getCubeBiomeMap(cube);

		this.noise = getCubeNoiseMap(cube);

		replaceBlocks(cube);
		return true;
	}

	private void replaceBlocks(final Cube cube) {
		this.rand.setSeed(41 * this.seed + cube.cubeRandomSeed());

		Cube cubeAbove = this.cache.getCube(cube.getX(), cube.getY() + 1, cube.getZ());
		BiomeBlockReplacer blockReplacer = new BiomeBlockReplacer(this.rand, cube, cubeAbove);

		for (int xRel = 0; xRel < 16; xRel++) {
			int xAbs = cube.getX() << 4 | xRel;

			for (int zRel = 0; zRel < 16; zRel++) {
				int zAbs = cube.getZ() << 4 | zRel;
				int xzCoord = zRel << 4 | xRel;

				// TODO: Reimplement this
				blockReplacer.replaceBlocks(this.biomes[xzCoord], xAbs, zAbs, this.noise[zRel * 16 + xRel]);
			}
		}
	}

	private double[] getCubeNoiseMap(final Cube cube) {
		return this.noiseGen.getNoiseMap(this.noise, Coords.cubeToMinBlock(cube.getX()),
				Coords.cubeToMinBlock(cube.getZ()), 16, 16, 16, 16, 1);
	}

	private Biome[] getCubeBiomeMap(final Cube cube) {
		// generate biome info. This is a hackjob.
		return cube.getWorld().dimension.getBiomeManager().getBiomeMap(this.biomes, Coords.cubeToMinBlock(cube.getX()),
				Coords.cubeToMinBlock(cube.getZ()), 16, 16);
	}

	private boolean canGenerate(Cube cube) {
		//cube above must exist and can't be before BIOMES stage.
		//also in the next stage need to make sure that we don't generate structures 
		//when biome blocks aren't placed in cube below
		int cubeX = cube.getX();
		int cubeY = cube.getY() + 1;
		int cubeZ = cube.getZ();
		boolean exists = this.cache.cubeExists(cubeX, cubeY, cubeZ);
		if (!exists) {
			return false;
		}
		Cube above = this.cache.getCube(cubeX, cubeY, cubeZ);
		return !above.getGeneratorStage().isLessThan(GeneratorStage.SURFACE);
	}
}
