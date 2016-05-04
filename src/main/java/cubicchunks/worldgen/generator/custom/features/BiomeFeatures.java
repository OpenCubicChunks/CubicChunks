/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.worldgen.generator.custom.features;

import cubicchunks.util.WorldProviderAccess;
import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.custom.features.trees.BigTreeGenerator;
import cubicchunks.worldgen.generator.custom.features.trees.SimpleTreeGenerator;
import cubicchunks.worldgen.generator.custom.features.trees.TreeGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.ChunkProviderSettings;

import java.util.ArrayList;
import java.util.Collection;

public class BiomeFeatures {
	private final ICubicWorld world;

	private final Collection<FeatureGenerator> generators;

	public BiomeFeatures(ICubicWorld world, BiomeGenBase biome) {
		this.world = world;
		this.generators = new ArrayList<>(20);
		BiomeDecorator decorator = biome.theBiomeDecorator;
		
		ChunkProviderSettings config = ChunkProviderSettings.Factory.jsonToFactory(
				WorldProviderAccess.getGeneratorSettings(world.getProvider())).build();
		
		//clay worldgen
		this.addMultiGen(SurfaceBlockReplacer.builder().
						world(world).height(1).radius(2).block(Blocks.CLAY).
						addAllowedAboveSurface(Blocks.WATER).
						addReplacable(Blocks.SAND).addReplacable(Blocks.DIRT).build(), decorator.clayPerChunk);
		
		//sand and gravel beach generators
//		this.addMultiGen(SurfaceBlockReplacer.builder().
//						world(world).height(1).radius(7).block(SAND).
//						addAllowedAboveSurface(WATER).
//						addReplacable(DIRT).addReplacable(GRASS).build(), decorator.sandBeachesPerChunk);
//		this.addMultiGen(SurfaceBlockReplacer.builder().
//						world(world).height(1).radius(6).block(GRAVEL).
//						addAllowedAboveSurface(WATER).
//						addReplacable(DIRT).addReplacable(GRASS).build(), decorator.gravelBeachesPerChunk);
		this.addTreeGenerators(decorator);
		addMultiGen(new TallGrassGenerator(world, BlockTallGrass.EnumType.GRASS), decorator.grassPerChunk);
		this.addOreGenerators(config);
	}

	protected final void addTreeGenerators(BiomeDecorator decorator) {
		//Other classes may override this methid to provide other tree generators
		TreeGenerator smallTreeGen = new SimpleTreeGenerator(world, Blocks.LOG.getDefaultState(), Blocks.LEAVES.getDefaultState());
		BigTreeGenerator bigTreeGen2 = new BigTreeGenerator(world, Blocks.LOG.getDefaultState(), Blocks.LEAVES.getDefaultState());
		bigTreeGen2.setHeightRange(28, 32);
		
		VariantFeatureGenerator randomTreeGen = VariantFeatureGenerator.builder()
						.nextVariant(smallTreeGen, 1.0)
						.build();
		
//		addMultiGen(randomTreeGen, decorator.treesPerChunk);
		addMultiGen(bigTreeGen2, decorator.treesPerChunk / 10);
	}
	
	protected final void addOreGenerators(ChunkProviderSettings cfg) {
		// it automatically scales with world height.
		// if min height is 0 - it assumes that there is no lower limit
		// if max height is 128 or 256 - it assumes there is no upper limit
		
		//ores
		addMineral(Blocks.COAL_ORE, cfg.coalMinHeight, cfg.coalMaxHeight, cfg.coalSize, cfg.coalCount);
		addMineral(Blocks.IRON_ORE, cfg.ironMinHeight, cfg.ironMaxHeight, cfg.ironSize, cfg.ironCount);
		addMineral(Blocks.GOLD_ORE, cfg.goldMinHeight, cfg.goldMaxHeight, cfg.goldSize, cfg.goldCount);
		addMineral(Blocks.REDSTONE_ORE, cfg.redstoneMinHeight, cfg.redstoneMaxHeight, cfg.redstoneSize, cfg.redstoneCount);
		addMineral(Blocks.DIAMOND_ORE, cfg.diamondMinHeight, cfg.diamondMaxHeight, cfg.diamondSize, cfg.diamondCount);

		//stone variants
		IBlockState stone = Blocks.STONE.getDefaultState();
		addMineral(stone.withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE), 
						cfg.andesiteMinHeight, cfg.andesiteMaxHeight, cfg.andesiteSize, cfg.andesiteCount);
		
		addMineral(stone.withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE), 
						cfg.dioriteMinHeight, cfg.dioriteMaxHeight, cfg.dioriteSize, cfg.dioriteCount);
		
		addMineral(stone.withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE), 
						cfg.graniteMinHeight, cfg.graniteMaxHeight, cfg.graniteSize, cfg.graniteCount);

		//other
		addMineral(Blocks.DIRT, cfg.dirtMinHeight, cfg.dirtMaxHeight, cfg.dirtSize, cfg.dirtCount);
//		addMineral(GRAVEL, cfg.gravelMinHeight, cfg.gravelMaxHeight, cfg.gravelSize, cfg.gravelCount);
	}

	protected final void addMineral(IBlockState state, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
		addMultiGen(new MineralGenerator(world, 
						state, 
						getMinHeight(vanillaMinHeight),
						getMaxHeight(vanillaMaxHeight), 
						size, 
						getProbability(vanillaMinHeight, vanillaMaxHeight)), countPerChunk);
	}
	
	protected final void addMineral(Block block, int vanillaMinHeight, int vanillaMaxHeight, int size, int countPerChunk){
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
