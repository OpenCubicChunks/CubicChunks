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

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cubicchunks.CubicChunks;
import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.ICubePrimer;

/**
 * A cube generator that tries to mirror vanilla world generation. Cubes in the normal world range will be copied from a
 * vanilla chunk generator, cubes above and below that will be filled with the most common block in the
 * topmost/bottommost layers.
 */
public class VanillaCompatibilityGenerator implements ICubeGenerator {

	private final int worldHeightBlocks;
	private final int worldHeightCubes;
	private IChunkGenerator vanilla;
	private ICubicWorld world;
	/**
	 * Last chunk that was generated from the vanilla world gen
	 */
	private Chunk lastChunk;
	/**
	 * We generate all the chunks in the vanilla range at once. This variable prevents infinite recursion
	 */
	private boolean optimizationHack;
	private Biome[] biomes;
	/**
	 * Detected block for filling cubes below the world
	 */
	private IBlockState extensionBlockBottom = Blocks.STONE.getDefaultState();
	/**
	 * Detected block for filling cubes above the world
	 */
	private IBlockState extensionBlockTop = Blocks.AIR.getDefaultState();

	/**
	 * Create a new VanillaCompatibilityGenerator
	 *
	 * @param vanilla The vanilla generator to mirror
	 * @param world The world in which cubes are being generated
	 */
	public VanillaCompatibilityGenerator(IChunkGenerator vanilla, ICubicWorld world) {
		this.vanilla = vanilla;
		this.world = world;

		// heuristics TODO: add a config that overrides this
		lastChunk = vanilla.provideChunk(0, 0); // lets scan the chunk at 0, 0

		worldHeightBlocks = world.getActualHeight();
		worldHeightCubes = worldHeightBlocks/Cube.SIZE;
		Map<IBlockState, Integer> blockHistogramBottom = new HashMap<>();
		Map<IBlockState, Integer> blockHistogramTop = new HashMap<>();

		for (int x = 0; x < Cube.SIZE; x++) {
			for (int z = 0; z < Cube.SIZE; z++) {
				// Scan three layers top / bottom each to guard against bedrock walls
				for (int y = 0; y < 3; y++) {
					IBlockState blockState = lastChunk.getBlockState(x, y, z);
					if (blockState.getBlock() == Blocks.BEDROCK) continue; // Never use bedrock for world extension

					int count = blockHistogramBottom.getOrDefault(blockState, 0);
					blockHistogramBottom.put(blockState, count + 1);
				}

				for (int y = worldHeightBlocks - 1; y > worldHeightBlocks - 4; y--) {
					IBlockState blockState = lastChunk.getBlockState(x, y, z);
					if (blockState.getBlock() == Blocks.BEDROCK) continue; // Never use bedrock for world extension

					int count = blockHistogramTop.getOrDefault(blockState, 0);
					blockHistogramTop.put(blockState, count + 1);
				}
			}
		}

		CubicChunks.LOGGER.debug("Block histograms: \nTop: " + blockHistogramTop + "\nBottom: " + blockHistogramBottom);

		int topcount = 0;
		for (Map.Entry<IBlockState, Integer> entry : blockHistogramBottom.entrySet()) {
			if (entry.getValue() > topcount) {
				extensionBlockBottom = entry.getKey();
				topcount = entry.getValue();
			}
		}
		CubicChunks.LOGGER.info("Detected filler block " + extensionBlockBottom.getBlock().getUnlocalizedName() + " " +
			"from layers [0, 2]");

		topcount = 0;
		for (Map.Entry<IBlockState, Integer> entry : blockHistogramTop.entrySet()) {
			if (entry.getValue() > topcount) {
				extensionBlockTop = entry.getKey();
				topcount = entry.getValue();
			}
		}
		CubicChunks.LOGGER.info("Detected filler block " + extensionBlockTop.getBlock().getUnlocalizedName() + " from" +
			" layers [" + (worldHeightBlocks - 3) + ", " + (worldHeightBlocks - 1) + "]");
	}

	@Override
	public void generateColumn(Column column) {

		this.biomes = this.world.getBiomeProvider()
			.getBiomes(this.biomes,
				Coords.cubeToMinBlock(column.getX()),
				Coords.cubeToMinBlock(column.getZ()),
				Cube.SIZE, Cube.SIZE);

		byte[] abyte = column.getBiomeArray();
		for (int i = 0; i < abyte.length; ++i) {
			abyte[i] = (byte) Biome.getIdForBiome(this.biomes[i]);
		}
	}

	@Override
	public void recreateStructures(Column column) {
		vanilla.recreateStructures(column, column.getX(), column.getZ());
	}

	@Override
	public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
		CubePrimer primer = new CubePrimer();

		if (cubeY < 0) {
			// Fill with bottom block
			for (int x = 0; x < Cube.SIZE; x++) {
				for (int y = 0; y < Cube.SIZE; y++) {
					for (int z = 0; z < Cube.SIZE; z++) {
						primer.setBlockState(x, y, z, extensionBlockBottom);
					}
				}
			}
		} else if (cubeY >= worldHeightCubes) {
			// Fill with top block
			for (int x = 0; x < Cube.SIZE; x++) {
				for (int y = 0; y < Cube.SIZE; y++) {
					for (int z = 0; z < Cube.SIZE; z++) {
						primer.setBlockState(x, y, z, extensionBlockTop);
					}
				}
			}
		} else {
			// Make vanilla generate a chunk for us to copy
			if (lastChunk.xPosition != cubeX || lastChunk.zPosition != cubeZ) {
				lastChunk = vanilla.provideChunk(cubeX, cubeZ);
			}

			if (!optimizationHack) {
				optimizationHack = true;
				// Recusrive generation
				for (int y = worldHeightCubes - 1; y >= 0; y--) {
					if (y == cubeY) {
						continue;
					}
					world.getCubeFromCubeCoords(cubeX, y, cubeZ);
				}
				optimizationHack = false;
			}

			// Copy from vanilla, replacing bedrock as appropriate
			ExtendedBlockStorage storage = lastChunk.getBlockStorageArray()[cubeY];
			if (storage != null && !storage.isEmpty()) {
				for (int x = 0; x < Cube.SIZE; x++) {
					for (int y = 0; y < Cube.SIZE; y++) {
						for (int z = 0; z < Cube.SIZE; z++) {
							IBlockState state = storage.get(x, y, z);
							if (state == Blocks.BEDROCK.getDefaultState()) {
								if (y < Cube.SIZE/2) {
									primer.setBlockState(x, y, z, extensionBlockBottom);
								} else {
									primer.setBlockState(x, y, z, extensionBlockTop);
								}
							} else {
								primer.setBlockState(x, y, z, state);
							}
						}
					}
				}
			}
		}

		return primer;
	}

	@Override
	public void populate(Cube cube) {
		// Cubes outside this range are only filled with their respective block
		// No population takes place
		if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) {
			for (int x = 0; x < 2; x++) {
				for (int z = 0; z < 2; z++) {
					for (int y = worldHeightCubes - 1; y >= 0; y--) {
						// Vanilla populators break the rules! They need to find the ground!
						world.getCubeFromCubeCoords(cube.getX() + x, y, cube.getZ() + z);
					}
				}
			}
			for (int y = worldHeightCubes - 1; y >= 0; y--) {
				// normal populators would not do this... but we are populating more than one cube!
				world.getCubeFromCubeCoords(cube.getX(), y, cube.getZ()).setPopulated(true);
			}

			vanilla.populate(cube.getX(), cube.getZ());
			GameRegistry.generateWorld(cube.getX(), cube.getZ(), (World) world, vanilla, ((World) world).getChunkProvider());
		}
	}

	@Override
	public Box getPopulationRequirement(Cube cube) {
		if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) {
			return new Box(
				-1, 0 - cube.getY(), -1,
				0, worldHeightCubes - cube.getY() - 1, 0
			);
		}
		return NO_POPULATOR_REQUIREMENT;
	}

	@Override
	public void recreateStructures(Cube cube) {
	}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		return vanilla.getPossibleCreatures(creatureType, pos);
	}

	@Override
	public BlockPos getClosestStructure(String name, BlockPos pos, boolean flag) {
		return vanilla.getStrongholdGen((World) world, name, pos, flag);
	}

}
