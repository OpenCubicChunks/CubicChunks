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

import cubicchunks.generator.features.trees.SimpleTreeGenerator;
import cubicchunks.generator.features.trees.BigTreeGenerator;
import static net.minecraft.block.Blocks.*;
import net.minecraft.block.BlockStone.EnumStoneVariant;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.Blocks;
import net.minecraft.block.state.IBlockState;
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
		//we need to generate big trees before small trees, so that they actually have some chance to generate
		addGen(new BigTreeGenerator(world, decorator.treesPerChunk, 0.1));
		addGen(new SimpleTreeGenerator(world, LOG.getDefaultState(), LEAVES.getDefaultState(), decorator.treesPerChunk, 0.9));
		addGen(new TallGrassGenerator(world, BlockTallGrass.TallGrassTypes.GRASS, decorator.randomGrassPerChunk));
		
		this.addOreGenerators(config);
	}

	private void addOreGenerators(GeneratorSettings cfg) {
		// it automatically scales with world height.
		// if min height is 0 - it assumes that there is no lower limit
		// if max height is 128 or 256 - it assumes there is no upper limit
		
		//ores
		addMineral(COAL_ORE, cfg.coalMinHeight, cfg.coalMaxHeight, cfg.coalSize, cfg.coalCount);
		addMineral(IRON_ORE, cfg.ironMinHeight, cfg.ironMaxHeight, cfg.ironSize, cfg.ironCount);
		addMineral(GOLD_ORE, cfg.goldMinHeight, cfg.goldMaxHeight, cfg.goldSize, cfg.goldCount);
		addMineral(REDSTONE_ORE, cfg.redstoneMinHeight, cfg.redstoneMaxHeight, cfg.redstoneSize, cfg.redstoneCount);
		addMineral(DIAMOND_ORE, cfg.diamondMinHeight, cfg.diamondMaxHeight, cfg.diamondSize, cfg.diamondCount);

		//stone variants
		// TODO: Actually we should write: Blocks.X.getDefaultState().setProperty(BlockX.type, BlockX.EnumXVariant.VARIANT);
		// but it's broken because of a bug in runtime obfuscator
		// explained in TallGrassGenerator
		addMineral(STONE.getBlockStateForMetadata(EnumStoneVariant.ANDESITE.getID()), 
						cfg.andesiteMinHeight, cfg.andesiteMaxHeight, cfg.andesiteSize, cfg.andesiteCount);
		
		addMineral(STONE.getBlockStateForMetadata(EnumStoneVariant.DIORITE.getID()), 
						cfg.dioriteMinHeight, cfg.dioriteMaxHeight, cfg.dioriteSize, cfg.dioriteCount);
		
		addMineral(STONE.getBlockStateForMetadata(EnumStoneVariant.GRANITE.getID()), 
						cfg.graniteMinHeight, cfg.graniteMaxHeight, cfg.graniteSize, cfg.graniteCount);

		//other
		addMineral(DIRT, cfg.dirtMinHeight, cfg.dirtMaxHeight, cfg.dirtSize, cfg.dirtCount);
		addMineral(GRAVEL, cfg.gravelMinHeight, cfg.gravelMaxHeight, cfg.gravelSize, cfg.gravelCount);
	}

	private void addMineral(IBlockState state, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
		addGen(new MineralGenerator(world, 
						state, 
						getMinHeight(vanillaMinHeight),
						getMaxHeight(vanillaMaxHeight), 
						size, 
						countPerChunk, 
						getProbability(vanillaMinHeight, vanillaMaxHeight)));
	}
	
	private void addMineral(Block block, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
		this.addMineral(block.getDefaultState(), vanillaMinHeight, vanillaMaxHeight, size, countPerChunk);
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
