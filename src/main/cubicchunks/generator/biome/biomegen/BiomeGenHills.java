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
import cubicchunks.generator.populator.generators.WorldGenTaiga2Cube;
import cubicchunks.world.Cube;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class BiomeGenHills extends CCBiome {
	private final WorldGenerator theWorldGenerator;
	private final WorldGenTaiga2Cube genTaiga;
	private final int typeDefault;
	private final int typeForest;
	private final int typeMutated;
	private int type;

	public BiomeGenHills(int biomeID, boolean isForest) {
		super(biomeID);
		this.theWorldGenerator = new WorldGenMinable(Blocks.monster_egg, 8);
		this.genTaiga = new WorldGenTaiga2Cube(false);
		this.typeDefault = 0;
		this.typeForest = 1;
		this.typeMutated = 2;
		this.type = this.typeDefault;

		if (isForest) {
			CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
			cfg.treesPerColumn(3);
			this.type = this.typeForest;
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree(Random rand) {
		return (rand.nextInt(3) > 0 ? this.genTaiga : super.checkSpawnTree(rand));
	}

	@Override
	public void decorate(World world, Random rand, int x, int y, int z) {
		super.decorate(world, rand, x, y, z);

		DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z);
		int numGen = 3 + rand.nextInt(6);

		gen.generateSingleBlocks(Blocks.emerald_ore, numGen, 1, -0.75);

		// generate silverfish stone (monsteregg)
		gen.generateAtRandomHeight(7, 1, theWorldGenerator, 0);
	}

	@Override
	public void replaceBlocks(World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom,
			int alterationTop, int seaLevel, double depthNoiseValue) {
		this.topBlock = Blocks.grass;
		this.field_150604_aj = 0;
		this.fillerBlock = Blocks.dirt;

		if ((depthNoiseValue < -1.0D || depthNoiseValue > 2.0D) && this.type == this.typeMutated) {
			this.topBlock = Blocks.gravel;
			this.fillerBlock = Blocks.gravel;
		} else if (depthNoiseValue > 1.0D && this.type != this.typeForest) {
			this.topBlock = Blocks.stone;
			this.fillerBlock = Blocks.stone;
		}

		this.replaceBlocks_do(world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel,
				depthNoiseValue);
	}

	@Override
	protected CCBiome createAndReturnMutated() {
		return (new BiomeGenHills(this.biomeID + 128, false)).createMutated(this);
	}

	private BiomeGenHills createMutated(CCBiome biome) {
		this.type = this.typeMutated;
		this.func_150557_a(biome.color, true);
		this.setBiomeName(biome.biomeName + " M");
		this.setHeightRange(new CCBiome.Height(biome.biomeHeight, biome.biomeVolatility));
		this.setTemperatureAndRainfall(biome.temperature, biome.rainfall);
		return this;
	}
}
