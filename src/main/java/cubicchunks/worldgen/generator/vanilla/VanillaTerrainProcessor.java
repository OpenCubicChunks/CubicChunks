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
package cubicchunks.worldgen.generator.vanilla;

import com.google.common.collect.Sets;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStage;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.gen.ChunkProviderSettings;

import java.util.HashSet;
import java.util.Set;

import static cubicchunks.util.ChunkProviderOverworldAccess.*;

public class VanillaTerrainProcessor extends CubeProcessor {
	private final ChunkProviderOverworld vanillaGen;
	private WorldServer world;
	private ServerCubeCache provider;

	public VanillaTerrainProcessor(WorldServer world, ServerCubeCache provider, ChunkProviderOverworld vanillaGen, int batchSize) {
		super("Terrain", provider, batchSize);
		this.world = world;
		this.provider = provider;
		this.vanillaGen = vanillaGen;
	}


	@Override public Set<Cube> calculate(Cube cube) {
		if (cube.getY() < 0) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						pos.set(x, y, z);
						cube.setBlockForGeneration(pos, Blocks.STONE.getDefaultState());
					}
				}
			}
			return Sets.newHashSet(cube);
		}
		if (cube.getY() >= 16) {
			return Sets.newHashSet(cube);
		}
		Set<Cube> cubes = new HashSet<>();
		for (int i = 0; i < 16; i++) {
			if (i != cube.getY()) {
				Cube newCube = this.provider.forceLoadCube(cube, cube.getX(), i, cube.getZ());
				cubes.add(newCube);
			}
		}
		cubes.add(cube);
		generateVanillaChunk(cube);
		return cubes;
	}

	private void generateVanillaChunk(Cube cube) {
		int x = cube.getX();
		int z = cube.getZ();
		getRand(this.vanillaGen).setSeed((long) x * 341873128712L + (long) z * 132897987541L);
		ChunkPrimer chunkprimer = new ChunkPrimer();

		setBlocksInChunk(this.vanillaGen, x, z, chunkprimer);
		BiomeGenBase[] newBiomes = this.world.getBiomeProvider().loadBlockGeneratorData(getBiomesForGeneration(this.vanillaGen), x * 16, z * 16, 16, 16);
		setBiomesForGeneration(this.vanillaGen, newBiomes);
		replaceBiomeBlocks(this.vanillaGen, x, z, chunkprimer, newBiomes);

		ChunkProviderSettings settings = getSettings(this.vanillaGen);
		if (settings.useCaves) {
			getCaveGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
		}

		if (settings.useRavines) {
			getRavineGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
		}

		if (getMapFeaturesEnabled(this.vanillaGen)) {
			if (settings.useMineShafts) {
				getMineshaftGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
			}

			if (settings.useVillages) {
				getVillageGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
			}

			if (settings.useStrongholds) {
				getStrongholdGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
			}

			if (settings.useTemples) {
				getScatteredFeatureGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
			}

			if (settings.useMonuments) {
				getOceanMonumentGenerator(this.vanillaGen).generate(this.world, x, z, chunkprimer);
			}
		}
		for(int cubeY = 0; cubeY < 16; cubeY++) {
			Cube currCube = this.provider.getCube(cube.getX(), cubeY, cube.getZ());
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for(int localX = 0; localX < 16; localX++) {
				for(int localY = 0; localY < 16; localY++) {
					for(int localZ = 0; localZ < 16; localZ++) {
						int blockY = Coords.localToBlock(cubeY, localY);
						pos.set(localX, localY, localZ);
						currCube.setBlockForGeneration(pos, chunkprimer.getBlockState(localX, blockY, localZ));
						currCube.setGeneratorStage(GeneratorStage.SURFACE);
					}
				}
			}
		}
	}

}
