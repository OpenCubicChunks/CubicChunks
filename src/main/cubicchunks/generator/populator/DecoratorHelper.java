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
package cubicchunks.generator.populator;

import cubicchunks.CubeWorldProvider;
import cubicchunks.generator.biome.biomegen.CCBiome;
import cubicchunks.generator.terrain.GlobalGeneratorConfig;
import cubicchunks.util.Coords;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenFlowersCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.FeatureGenerator;

public class DecoratorHelper {
	public final World world;
	public final int chunk_X, chunk_Y, chunk_Z;
	private final Random rand;
	private final int seaLevel;
	private final int maxTerrainY;

	public DecoratorHelper(World world, Random rand, int x, int y, int z) {
		this.world = world;
		this.chunk_X = x;
		this.chunk_Y = y;
		this.chunk_Z = z;
		this.rand = rand;
		this.seaLevel = ((CubeWorldProvider) world.provider).getSeaLevel();
		this.maxTerrainY = MathHelper.floor_double(GlobalGeneratorConfig.maxElev);
	}

	/**
	 * Generation helper. Generates everything that should be generated on
	 * surface.
	 * 
	 * @param generator
	 *            This generator will be used
	 * @param numGen
	 *            Generation attempts
	 * @param probability
	 *            Probability to generate in generation attempt
	 */
	public boolean generateAtSurface(FeatureGenerator generator, int numGen, double probability) {
		boolean generated = false;
		int minY = Coords.cubeToMinBlock(chunk_Y) + 8;
		int maxY = minY + 16;

		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		for (int i = 0; i < numGen; ++i) {
			if (this.rand.nextDouble() > probability) {
				continue;
			}
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);
			int yAbs = this.world.getTopSolidOrLiquidBlock(xAbs, zAbs);

			if (yAbs < minY || yAbs > maxY) {
				continue;
			}

			if (generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs)
					&& generator instanceof WorldGenAbstractTreeCube) {
				((WorldGenAbstractTreeCube) generator).afterGenerate(this.world, this.rand, xAbs, yAbs, zAbs);
			}

		}
		return generated;
	}

	/**
	 * Generation helper. Generates everything for which in vailla random height
	 * is calculated like this: int y = rand.nextInt( world.getHeightValue( x, z
	 * ) + 32 );
	 * 
	 * @param generator
	 *            This generator will be used
	 * @param numGen
	 *            Generation attempts
	 * @param probability
	 *            Probability to generate in generation attempt
	 */
	public boolean generateAtRandSurfacePlus32(FeatureGenerator generator, int numGen, double probability) {
		if (this.chunk_Y < 0) {
			return false;
		}
		boolean generated = false;
		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		for (int i = 0; i < numGen; i++) {
			double genProb = probability;
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int yAbs = blockYCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			double heightBlocks = this.world.getHeightValue(xAbs, zAbs);
			double height = heightBlocks / maxTerrainY;

			genProb *= 1.0D / (4.0D * height + 6.0D);

			if (yAbs > heightBlocks + 32 || this.rand.nextDouble() > genProb) {
				continue;
			}

			generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	/**
	 * Generation helper. Generates everything for which in vailla random height
	 * is calculated like this: int y = rand.nextInt( world.getHeightValue( x, z
	 * ) * 2 ); And generator finds top block below y
	 * 
	 * @param generator
	 *            This generator will be used
	 * @param numGen
	 *            Generation attempts
	 * @param probability
	 *            Probability to generate in generation attempt
	 */
	public boolean generateAtRand2xHeight1(FeatureGenerator generator, int numGen, double probability) {
		boolean generated = false;
		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		probability *= 0.5;

		for (int i = 0; i < numGen; i++) {
			if (this.rand.nextDouble() > probability) {
				continue;
			}

			int xAbs = blockXCenter + this.rand.nextInt(16);
			int yAbs = blockYCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	/**
	 * Generation helper. Generates everything for which in vailla random height
	 * is calculated like this: int y = rand.nextInt( world.getHeightValue( x, z
	 * ) * 2 ); Finds top block below y
	 * 
	 * @param generator
	 *            This generator will be used
	 * @param numGen
	 *            Generation attempts
	 * @param probability
	 *            Probability to generate in generation attempt
	 */
	public boolean generateAtRand2xHeight2(FeatureGenerator generator, int numGen, double probability) {
		boolean generated = false;
		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		int minY = Coords.cubeToMinBlock(chunk_Y) + 8;
		int maxY = minY + 16;

		probability *= 0.5;

		generatorLoop: for (int i = 0; i < numGen; i++) {
			if (this.rand.nextDouble() > probability) {
				continue;
			}

			int yAbs = blockYCenter + this.rand.nextInt(16);

			if (yAbs < seaLevel - 1) {
				continue;
			}

			int xAbs = blockXCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			while (this.world.isAirBlock(xAbs, yAbs - 1, zAbs)) {
				if (--yAbs < minY) {
					continue generatorLoop;
				}
			}

			assert yAbs >= minY && yAbs <= maxY;

			generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	/**
	 * Generation helper. Generates everything for which in vailla random height
	 * is calculated like this: int y = rand.nextInt( world.getHeightValue( x, z
	 * ) * 2 ); For all other generators, that don't touch given x/y/z locations
	 * 
	 * @param generator
	 *            This generator will be used
	 * @param numGen
	 *            Generation attempts
	 * @param probability
	 *            Probability to generate in generation attempt
	 */
	public boolean generateAtRand2xHeight3(FeatureGenerator generator, int numGen, double probability) {
		boolean generated = false;
		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		for (int i = 0; i < numGen; i++) {
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int yAbs = blockYCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			double heightBlocks = this.world.getHeightValue(xAbs, zAbs);
			// if it's > 32 blocks above surface nothing will be generated. Do
			// don't try to generate it.
			if (yAbs > heightBlocks + 32) {
				continue;
			}
			double genProb = probability;

			double height = heightBlocks / maxTerrainY;

			genProb *= 1.0D / (8.0D * height + 8.0D);

			// at -maxTerrainY genProb is infinite, so it will be always
			// generated (unless probability is 0). Below -maxTerrainY it's
			// negative.
			// Let the probability to decrease as we go deeper below this
			// height. It will never be 0.
			genProb = Math.abs(genProb);

			// when probability is 0 and y == -maxTerrainY genProb is NaN. x
			// >/</== NaN is always false.
			if (rand.nextDouble() > genProb) {
				continue;
			}

			generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	public boolean generateFlowers(WorldGenFlowersCube generator, CCBiome biome, int numGen, double probability) {
		boolean generated = false;
		int blockXCenter = Coords.cubeToMinBlock(chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(chunk_Z) + 8;

		for (int i = 0; i < numGen; i++) {
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int yAbs = blockYCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			double heightBlocks = this.world.getHeightValue(xAbs, zAbs);
			double height = heightBlocks / maxTerrainY;

			probability *= 1.0D / (4.0D * height + 6.0D);

			if (yAbs > heightBlocks + 32 || this.rand.nextDouble() > probability) {
				continue;
			}

			String name = biome.spawnFlower(this.rand, xAbs, yAbs, zAbs);
			BlockFlower flower = BlockFlower.func_149857_e(name);

			if (flower.getMaterial() != Material.air) {
				generator.setTypeMetadata(flower, BlockFlower.func_149856_f(name));
				generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
			}
		}
		return generated;
	}

	public boolean generateWater() {
		boolean generated = false;
		double maxProb = 0.235;
		double minProb = 0.0000035 * 255 * 255 - 0.0018 * 255 + maxProb;
		for (int i = 0; i < 50; ++i) {
			double h = Coords.cubeToMaxBlock(chunk_Y) / (double) maxTerrainY;
			if (h > 1) {
				continue;
			}
			double height = 64 * (h + 1);
			// Don't touch it.
			double prob = 0.0000035 * height * height - 0.0018 * height + maxProb;
			prob = MathHelper.clamp_double(prob, minProb, maxProb);
			if (this.rand.nextDouble() > prob) {
				continue;
			}
			int xAbs = this.chunk_X * 16 + this.rand.nextInt(16) + 8;
			// var5 = this.rand.nextInt( this.rand.nextInt( 248 ) + 8 );
			int yAbs = this.chunk_Y * 16 + this.rand.nextInt(16) + 8;
			int zAbs = this.chunk_Z * 16 + this.rand.nextInt(16) + 8;
			generated |= (new WorldGenLiquids(Blocks.flowing_water)).generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	public boolean generateLava() {
		boolean generated = false;
		double maxProb = 0.235;
		double minProb = 0.0000035 * 255 * 255 - 0.0018 * 255 + maxProb;
		for (int i = 0; i < 50; ++i) {
			double h = Coords.cubeToMaxBlock(chunk_Y) / (double) maxTerrainY;
			if (h > 1) {
				continue;
			}
			double height = 64 * (h + 1);
			// Don't touch it.
			double prob = 0.0000035 * height * height - 0.0018 * height + maxProb;
			prob = MathHelper.clamp_double(prob, minProb, maxProb);

			if (h > seaLevel / (double) maxTerrainY) {
				prob *= prob;
			} else {
				prob = Math.sqrt(prob);
			}

			if (this.rand.nextDouble() > prob) {
				continue;
			}
			int xAbs = this.chunk_X * 16 + this.rand.nextInt(16) + 8;
			// var5 = this.rand.nextInt( this.rand.nextInt( 248 ) + 8 );
			int yAbs = this.chunk_Y * 16 + this.rand.nextInt(16) + 8;
			int zAbs = this.chunk_Z * 16 + this.rand.nextInt(16) + 8;
			generated |= (new WorldGenLiquids(Blocks.flowing_water)).generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	/**
	 * Generation helper. Generates ores.
	 */
	public boolean generateAtRandomHeight(int numGen, double probability, FeatureGenerator generator, double minHeight,
			double maxHeight) {
		boolean generated = false;
		final double minBlockY = Double.isNaN(minHeight) ? -Double.MAX_VALUE : minHeight * maxTerrainY;
		final double maxBlockY = Double.isNaN(maxHeight) ? Double.MAX_VALUE : maxHeight * maxTerrainY;

		int blockXCenter = Coords.cubeToMinBlock(this.chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(this.chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(this.chunk_Z) + 8;
		for (int n = 0; n < numGen; ++n) {
			if (this.rand.nextDouble() > probability) {
				continue;
			}
			int yAbs = blockYCenter + this.rand.nextInt(16);
			if (yAbs > maxBlockY || yAbs < minBlockY) {
				continue;
			}
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);

			generated |= generator.generate(this.world, this.rand, xAbs, yAbs, zAbs);
		}
		return generated;
	}

	public boolean generateAtRandomHeight(int numGen, double probability, FeatureGenerator generator, double maxHeight) {
		return this.generateAtRandomHeight(numGen, probability, generator, Double.NaN, maxHeight);
	}

	public boolean genberateAtRandomHeight(int numGen, double probability, FeatureGenerator generator) {
		return this.generateAtRandomHeight(numGen, probability, generator, Double.NaN, Double.NaN);
	}

	public boolean generateSingleBlocks(Block block, int numGen, double probability, double minHeight, double maxHeight) {
		boolean generated = false;
		final double minBlockY = Double.isNaN(minHeight) ? -Double.MAX_VALUE : minHeight * maxTerrainY;
		final double maxBlockY = Double.isNaN(maxHeight) ? Double.MAX_VALUE : maxHeight * maxTerrainY;

		int blockXCenter = Coords.cubeToMinBlock(this.chunk_X) + 8;
		int blockYCenter = Coords.cubeToMinBlock(this.chunk_Y) + 8;
		int blockZCenter = Coords.cubeToMinBlock(this.chunk_Z) + 8;

		for (int i = 0; i < numGen; ++i) {
			if (this.rand.nextDouble() > probability) {
				continue;
			}
			int yAbs = blockYCenter + this.rand.nextInt(16);
			if (yAbs > maxBlockY || yAbs < minBlockY) {
				continue;
			}
			int xAbs = blockXCenter + this.rand.nextInt(16);
			int zAbs = blockZCenter + this.rand.nextInt(16);
			if (world.getBlock(xAbs, yAbs, zAbs) == Blocks.stone) {
				world.setBlock(xAbs, yAbs, zAbs, Blocks.emerald_ore, 0, 2);
				generated = true;
			}
		}
		return generated;
	}

	public boolean generateSingleBlocks(Block emerald_ore, int numGen, int probability, double maxHeight) {
		return this.generateSingleBlocks(emerald_ore, numGen, probability, Double.NaN, maxHeight);
	}

	public boolean generateSingleBlocks(Block emerald_ore, int numGen, int probability) {
		return this.generateSingleBlocks(emerald_ore, numGen, probability, Double.NaN, Double.NaN);
	}
}
