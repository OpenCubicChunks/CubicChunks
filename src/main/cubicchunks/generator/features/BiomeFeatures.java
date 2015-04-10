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

package cubicchunks.generator.features;

import static net.minecraft.block.Blocks.*;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.decorator.Decorator;
import net.minecraft.world.gen.GeneratorSettings;

public class BiomeFeatures {
	private final World world;

	private final Collection<FeatureGenerator> generators;

	public BiomeFeatures(World world, Biome biome) {
		this.world = world;
		this.generators = new ArrayList<FeatureGenerator>(20);

		Decorator decorator = biome.biomeDecorator;
		GeneratorSettings config = GeneratorSettings.GeneratorSettingsFactory.createWithOptions(
				world.dimension.generatorOptions).getGeneratorSettings();

		addGen(new SimpleTreeGenerator(world, LOG.defaultState, LEAVES.defaultState, decorator.treesPerChunk, 1));
		addGen(new TallGrassGenerator(world, BlockTallGrass.TallGrassTypes.GRASS, decorator.randomGrassPerChunk));

		this.addOreGenerators(config);
	}

	private void addOreGenerators(GeneratorSettings config) {
		// it automatically scales with world height.
		// if min height is 0 - it assumes that there is no lower limit
		// if max height is 128 or 256 - it assumes there is no upper limit
		addGen(new OreGenerator(world, 
						Blocks.COAL_ORE.defaultState, 
						getMinHeight(config.coalMinHeight),
						getMaxHeight(config.coalMaxHeight), 
						config.coalSize, 
						config.coalCount, 
						getProbability(config.coalMinHeight, config.coalMaxHeight)));

		addGen(new OreGenerator(world, 
						Blocks.IRON_ORE.defaultState, 
						getMinHeight(config.ironMinHeight),
						getMaxHeight(config.ironMaxHeight), 
						config.ironSize, 
						config.ironCount, 
						getProbability(config.ironMinHeight, config.ironMaxHeight)));

		addGen(new OreGenerator(world, 
						Blocks.GOLD_ORE.defaultState, 
						getMinHeight(config.goldMinHeight),
						getMaxHeight(config.goldMaxHeight), 
						config.goldSize, 
						config.goldCount, 
						getProbability(config.goldMinHeight, config.goldMaxHeight)));
		
		addGen(new OreGenerator(world, 
						Blocks.REDSTONE_ORE.defaultState, 
						getMinHeight(config.redstoneMinHeight),
						getMaxHeight(config.redstoneMaxHeight), 
						config.redstoneSize, 
						config.redstoneCount, 
						getProbability(config.redstoneMinHeight, config.redstoneMaxHeight)));
		
		addGen(new OreGenerator(world, 
						Blocks.DIAMOND_ORE.defaultState, 
						getMinHeight(config.diamondMinHeight),
						getMaxHeight(config.diamondMaxHeight), 
						config.diamondSize, 
						config.diamondCount, 
						getProbability(config.diamondMinHeight, config.diamondMaxHeight)));
	}

	private double getMinHeight(int vanillaHeight) {
		if (vanillaHeight == 0) {
			// extend down to infinity
			return -Double.MAX_VALUE;
		}
		return (vanillaHeight - 64.0) / 64.0;
	}

	private double getMaxHeight(int vanillaHeight) {
		if (vanillaHeight == 128 || vanillaHeight == 256) {
			// extend up to infinity
			return Double.MAX_VALUE;
		}
		return (vanillaHeight - 64.0) / 64.0;
	}

	private double getProbability(int minY, int maxY) {
		return 16.0 / (maxY - minY);
	}

	protected final void addGen(FeatureGenerator gen) {
		this.generators.add(gen);
	}

	public Collection<FeatureGenerator> getBiomeFeatureGenerators() {
		return generators;
	}
}
