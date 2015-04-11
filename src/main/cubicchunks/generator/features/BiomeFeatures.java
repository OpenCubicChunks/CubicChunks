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

import cubicchunks.generator.features.trees.BigTreeGenerator;
import cubicchunks.generator.features.trees.SimpleTreeGenerator;
import cubicchunks.generator.features.trees.TreeGenerator;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone.EnumStoneVariant;
import net.minecraft.block.BlockTallGrass;
import static net.minecraft.block.Blocks.AIR;
import static net.minecraft.block.Blocks.CLAY;
import static net.minecraft.block.Blocks.COAL_ORE;
import static net.minecraft.block.Blocks.DIAMOND_ORE;
import static net.minecraft.block.Blocks.DIRT;
import static net.minecraft.block.Blocks.GOLD_ORE;
import static net.minecraft.block.Blocks.GRASS;
import static net.minecraft.block.Blocks.GRAVEL;
import static net.minecraft.block.Blocks.IRON_ORE;
import static net.minecraft.block.Blocks.LEAVES;
import static net.minecraft.block.Blocks.LOG;
import static net.minecraft.block.Blocks.REDSTONE_ORE;
import static net.minecraft.block.Blocks.SAND;
import static net.minecraft.block.Blocks.STONE;
import static net.minecraft.block.Blocks.WATER;
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
		
		//clay generator
		this.addMultiGen(SurfaceBlockReplacer.builder().
						world(world).height(1).radius(2).block(CLAY).
						addAllowedAboveSurface(WATER).
						addReplacable(SAND).addReplacable(DIRT).build(), decorator.clayPerChunk);
		
		//sand and gravel beach generators
		this.addMultiGen(SurfaceBlockReplacer.builder().
						world(world).height(1).radius(7).block(SAND).
						addAllowedAboveSurface(WATER).
						addReplacable(DIRT).addReplacable(GRASS).build(), decorator.sandBeachesPerChunk);
		this.addMultiGen(SurfaceBlockReplacer.builder().
						world(world).height(1).radius(6).block(GRAVEL).
						addAllowedAboveSurface(WATER).
						addReplacable(DIRT).addReplacable(GRASS).build(), decorator.gravelBeachesPerChunk);
		this.addTreeGenerators(decorator);
		addMultiGen(new TallGrassGenerator(world, BlockTallGrass.TallGrassTypes.GRASS), decorator.randomGrassPerChunk);
		this.addOreGenerators(config);
	}

	protected void addTreeGenerators(Decorator decorator) {
		//Other classes may override this methid to provide other tree generators
		TreeGenerator bigTreeGen = new BigTreeGenerator(world);
		TreeGenerator smallTreeGen = new SimpleTreeGenerator(world, LOG.getDefaultState(), LEAVES.getDefaultState());
		
		VariantFeatureGenerator randomTreeGen = VariantFeatureGenerator.builder()
						.nextVariant(bigTreeGen, 0.1)
						.nextVariant(smallTreeGen, 1.0)
						.build();
		
		addMultiGen(randomTreeGen, decorator.treesPerChunk);
	}
	
	protected void addOreGenerators(GeneratorSettings cfg) {
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

	protected void addMineral(IBlockState state, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
		addMultiGen(new MineralGenerator(world, 
						state, 
						getMinHeight(vanillaMinHeight),
						getMaxHeight(vanillaMaxHeight), 
						size, 
						getProbability(vanillaMinHeight, vanillaMaxHeight)), countPerChunk);
	}
	
	protected void addMineral(Block block, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
		this.addMineral(block.getDefaultState(), vanillaMinHeight, vanillaMaxHeight, size, countPerChunk);
	}

	protected final void addGen(FeatureGenerator gen) {
		this.generators.add(gen);
	}
	
	protected final void addMultiGen(FeatureGenerator gen, int attempts) {
		this.generators.add(new MultiFeatureGenerator(this.world, gen, attempts));
	}

	public Collection<FeatureGenerator> getBiomeFeatureGenerators() {
		return generators;
	}
	
	private static double getMinHeight(int vanillaHeight) {
		if (vanillaHeight == 0) {
			// extend down to infinity
			return -Double.MAX_VALUE;
		}
		return (vanillaHeight - 64.0) / 64.0;
	}

	private static double getMaxHeight(int vanillaHeight) {
		if (vanillaHeight == 128 || vanillaHeight == 256) {
			// extend up to infinity
			return Double.MAX_VALUE;
		}
		return (vanillaHeight - 64.0) / 64.0;
	}

	private static double getProbability(int minY, int maxY) {
		return 16.0 / (maxY - minY);
	}
}
