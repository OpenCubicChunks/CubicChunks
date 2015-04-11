/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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

import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import cubicchunks.generator.noise.NoiseGeneratorPerlin;
import cubicchunks.generator.terrain.GlobalGeneratorConfig;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.biome.BiomeBlockReplacer;
import cubicchunks.world.cube.Cube;

public class BiomeProcessor extends CubeProcessor {
	
	private WorldServer worldServer;
	
	private Random rand;
	private NoiseGeneratorPerlin m_noiseGen;
	private double[] noise;
	private Biome[] biomes;
	
	private int seaLevel;
	
	public BiomeProcessor(String name, WorldServer worldServer, ICubeCache cubeCache, int batchSize) {
		super(name, cubeCache, batchSize);
		
		this.worldServer = worldServer;
		
		this.rand = new Random(worldServer.getSeed());
		this.m_noiseGen = new NoiseGeneratorPerlin(this.rand, 4);
		this.noise = new double[256];
		this.biomes = null;
		
		this.seaLevel = GlobalGeneratorConfig.SEA_LEVEL;
	}
	
	@Override
	public boolean calculate(Cube cube) {
		
		// if the cube is empty, there is nothing to do. Even if neighbors don't exist
		if(cube.isEmpty()) {
			return true;
		}
		
		// only continue if the neighboring cubes are at least in the biome stage
		WorldContext worldContext = WorldContext.get(cube.getWorld());
		if (!worldContext.cubeAndNeighborsExist(cube, true, GeneratorStage.BIOMES)) {
			return false;
		}
		
		rand.setSeed(41 * cube.getWorld().getSeed() + cube.cubeRandomSeed());
		
		// generate biome info. This is a hackjob.
		this.biomes = cube.getWorld().dimension.getBiomeManager().getBiomeMap(
			this.biomes, 
			Coords.cubeToMinBlock(cube.getX()), 
			Coords.cubeToMinBlock(cube.getZ()), 
			16, 16
		);
		
		this.noise = this.m_noiseGen.arrayNoise2D_pre(
			this.noise, 
			Coords.cubeToMinBlock(cube.getX()), 
			Coords.cubeToMinBlock(cube.getZ()), 
			16, 16,
			16, 16,
			1
		);
		
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
				blockReplacer.replaceBlocks(this.worldServer, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, noise[zRel * 16 + xRel]);
			}
		}
		return true;
	}
}
