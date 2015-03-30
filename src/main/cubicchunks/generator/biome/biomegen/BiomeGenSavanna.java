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
package cubicchunks.generator.biome.biomegen;

import cubicchunks.generator.populator.DecoratorHelper;
import cubicchunks.generator.populator.WorldGenAbstractTreeCube;
import cubicchunks.generator.populator.generators.WorldGenSavannaTreeCube;
import cubicchunks.world.Cube;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class BiomeGenSavanna extends CCBiome {
	private static final WorldGenSavannaTreeCube savannaTreeGen = new WorldGenSavannaTreeCube(false);

	@SuppressWarnings("unchecked")
	protected BiomeGenSavanna(int id) {
		super(id);
		this.spawnableCreatureList.add(new CCBiome.SpawnListEntry(EntityHorse.class, 1, 2, 6));

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn(1);
		cfg.flowersPerColumn(4);
		cfg.grassPerColumn(20);
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree(Random rand) {
		return rand.nextInt(5) > 0 ? savannaTreeGen : this.worldGeneratorTrees;
	}

	@Override
	public void decorate(World world, Random rand, int x, int y, int z) {
		DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z);

		worldGenDoublePlant.setType(2);
		gen.generateAtRandSurfacePlus32(worldGenDoublePlant, 7, 1);

		super.decorate(world, rand, x, y, z);
	}

	@Override
	protected CCBiome createAndReturnMutated() {
		BiomeGenSavanna.Mutated biome = new BiomeGenSavanna.Mutated(this.biomeID + 128, this);
		biome.temperature = (this.temperature + 1.0F) * 0.5F;
		biome.biomeHeight = this.biomeHeight * 0.5F + 0.3F;
		biome.biomeVolatility = this.biomeVolatility * 0.5F + 1.2F;
		return biome;
	}

	public static class Mutated extends BiomeGenMutated {
		public Mutated(int biomeID, CCBiome newBiome) {
			super(biomeID, newBiome);

			CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

			cfg.treesPerColumn(2);
			cfg.flowersPerColumn(2);
			cfg.grassPerColumn(5);
		}

		@Override
		public void replaceBlocks(World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top,
				int bottom, int alterationTop, int seaLevel, double depthNoiseValue) {

			this.topBlock = Blocks.grass;
			this.field_150604_aj = 0;
			this.fillerBlock = Blocks.dirt;

			if (depthNoiseValue > 1.75D) {
				this.topBlock = Blocks.stone;
				this.fillerBlock = Blocks.stone;
			} else if (depthNoiseValue > -0.5D) {
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 1;
			}

			super.replaceBlocks(world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel,
					depthNoiseValue);
		}

		@Override
		public void decorate(World world, Random rand, int x, int y, int z) {
			this.decorator().decorate(world, rand, this, x, y, z);
		}
	}
}