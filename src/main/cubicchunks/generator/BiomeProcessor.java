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
import cubicchunks.generator.noise.NoiseGeneratorPerlin;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.biome.BiomeBlockReplacer;
import cubicchunks.world.cube.Cube;

public class BiomeProcessor extends CubeProcessor {
	
	private static final String PROCESSOR_NAME = "Biomes";
	
	private Random rand;
	private NoiseGeneratorPerlin noiseGen;
	private double[] noise;
	private Biome[] biomes;
	private long seed;
	
	public BiomeProcessor(final ICubeCache cubeCache, final int batchSize, final long seed) {
		super(PROCESSOR_NAME, cubeCache, batchSize);
		
		this.rand = new Random(seed);
		this.noiseGen = new NoiseGeneratorPerlin(this.rand, 4);
		this.noise = new double[256];
		this.biomes = null;
		this.seed = seed;
	}
	
	@Override
	public boolean calculate(final Cube cube) {
		
		// if the cube is empty, there is nothing to do. Even if neighbors don't exist
		if(cube.isEmpty()) {
			return true;
		}
		
		// only continue if the neighboring cubes are at least in the biome stage
		WorldContext worldContext = WorldContext.get(cube.getWorld());
		if (!worldContext.cubeAndNeighborsExist(cube, true, GeneratorStage.BIOMES)) {
			return false;
		}
		
		this.biomes = getCubeBiomeMap(cube);
		
		this.noise = getCubeNoiseMap(cube);
		
		replaceBlocks(cube);
		return true;
	}

	private void replaceBlocks(final Cube cube) {
		this.rand.setSeed(41 * this.seed + cube.cubeRandomSeed());
		
		int topOfCube = Coords.cubeToMaxBlock(cube.getY());
		int topOfCubeAbove = Coords.cubeToMaxBlock(cube.getY() + 1);
		int bottomOfCube = Coords.cubeToMinBlock(cube.getY());
		
		// already checked that cubes above and below exist
		int alterationTop = topOfCube;
		int top = topOfCubeAbove;
		int bottom = bottomOfCube;
		
		Cube above = this.cache.getCube(cube.getX(), cube.getY() + 1, cube.getZ());
		
		for (int xRel = 0; xRel < 16; xRel++) {
			int xAbs = cube.getX() << 4 | xRel;
			
			for (int zRel = 0; zRel < 16; zRel++) {
				int zAbs = cube.getZ() << 4 | zRel;
				int xzCoord = zRel << 4 | xRel;
				
				//TODO: Reimplement this
				BiomeBlockReplacer blockReplacer = new BiomeBlockReplacer(this.biomes[xzCoord]);
				blockReplacer.replaceBlocks(this.rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, this.noise[zRel * 16 + xRel]);
			}
		}
	}

	private double[] getCubeNoiseMap(final Cube cube) {
		return this.noiseGen.arrayNoise2D_pre(
			this.noise, 
			Coords.cubeToMinBlock(cube.getX()), 
			Coords.cubeToMinBlock(cube.getZ()), 
			16, 16,
			16, 16,
			1
		);
	}

	private Biome[] getCubeBiomeMap(final Cube cube) {
		// generate biome info. This is a hackjob.
		return cube.getWorld().dimension.getBiomeManager().getBiomeMap(
			this.biomes, 
			Coords.cubeToMinBlock(cube.getX()), 
			Coords.cubeToMinBlock(cube.getZ()), 
			16, 16
		);
	}
}
