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
package io.github.opencubicchunks.cubicchunks.core.worldgen.generator.vanilla;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A cube generator that tries to mirror vanilla world generation. Cubes in the normal world range will be copied from a
 * vanilla chunk generator, cubes above and below that will be filled with the most common block in the
 * topmost/bottommost layers.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VanillaCompatibilityGenerator implements ICubeGenerator {

    private boolean isInit = false;
    private int worldHeightBlocks;
    private int worldHeightCubes;
    @Nonnull private IChunkGenerator vanilla;
    @Nonnull private World world;
    /**
     * Last chunk that was generated from the vanilla world gen
     */
    @Nonnull private Chunk lastChunk;
    /**
     * We generate all the chunks in the vanilla range at once. This variable prevents infinite recursion
     */
    private boolean optimizationHack;
    private Biome[] biomes;
    /**
     * Detected block for filling cubes below the world
     */
    @Nonnull private IBlockState extensionBlockBottom = Blocks.STONE.getDefaultState();
    /**
     * Detected block for filling cubes above the world
     */
    @Nonnull private IBlockState extensionBlockTop = Blocks.AIR.getDefaultState();

    /**
     * Create a new VanillaCompatibilityGenerator
     *
     * @param vanilla The vanilla generator to mirror
     * @param world The world in which cubes are being generated
     */
    public VanillaCompatibilityGenerator(IChunkGenerator vanilla, World world) {
        this.vanilla = vanilla;
        this.world = world;
    }

    // lazy initialization to avoid circular dependencies
    private void tryInit(IChunkGenerator vanilla, World world) {
        if (isInit) {
            return;
        }
        isInit = true;
        // heuristics TODO: add a config that overrides this
        lastChunk = vanilla.generateChunk(0, 0); // lets scan the chunk at 0, 0

        worldHeightBlocks = ((ICubicWorld) world).getMaxGenerationHeight();
        worldHeightCubes = worldHeightBlocks / Cube.SIZE;
        Map<IBlockState, Integer> blockHistogramBottom = new HashMap<>();
        Map<IBlockState, Integer> blockHistogramTop = new HashMap<>();

        for (int x = 0; x < Cube.SIZE; x++) {
            for (int z = 0; z < Cube.SIZE; z++) {
                // Scan three layers top / bottom each to guard against bedrock walls
                for (int y = 0; y < 3; y++) {
                    IBlockState blockState = lastChunk.getBlockState(x, y, z);
                    if (blockState.getBlock() == Blocks.BEDROCK) {
                        continue; // Never use bedrock for world extension
                    }

                    int count = blockHistogramBottom.getOrDefault(blockState, 0);
                    blockHistogramBottom.put(blockState, count + 1);
                }

                for (int y = worldHeightBlocks - 1; y > worldHeightBlocks - 4; y--) {
                    IBlockState blockState = lastChunk.getBlockState(x, y, z);
                    if (blockState.getBlock() == Blocks.BEDROCK) {
                        continue; // Never use bedrock for world extension
                    }

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
    public void generateColumn(Chunk column) {

        this.biomes = this.world.getBiomeProvider()
                .getBiomes(this.biomes,
                        Coords.cubeToMinBlock(column.x),
                        Coords.cubeToMinBlock(column.z),
                        Cube.SIZE, Cube.SIZE);

        byte[] abyte = column.getBiomeArray();
        for (int i = 0; i < abyte.length; ++i) {
            abyte[i] = (byte) Biome.getIdForBiome(this.biomes[i]);
        }
    }

    @Override
    public void recreateStructures(Chunk column) {
        vanilla.recreateStructures(column, column.x, column.z);
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        tryInit(vanilla, world);
        CubePrimer primer = new CubePrimer();

        if (cubeY < 0) {
            Random rand = new Random(world.getSeed());
            rand.setSeed(rand.nextInt() ^ cubeX);
            rand.setSeed(rand.nextInt() ^ cubeZ);
            // Fill with bottom block
            for (int x = 0; x < Cube.SIZE; x++) {
                for (int y = 0; y < Cube.SIZE; y++) {
                    for (int z = 0; z < Cube.SIZE; z++) {
                        IBlockState state = extensionBlockBottom;
                        if (state.getBlock() != Blocks.AIR) {
                            int blockY = Coords.localToBlock(cubeY, y);
                            state = WorldGenUtils.getRandomBedrockReplacement(world, rand, state, blockY, 5);
                        }
                        primer.setBlockState(x, y, z, state);
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
            if (lastChunk.x != cubeX || lastChunk.z != cubeZ) {
                lastChunk = vanilla.generateChunk(cubeX, cubeZ);
            }

            if (!optimizationHack) {
                optimizationHack = true;
                // Recusrive generation
                for (int y = worldHeightCubes - 1; y >= 0; y--) {
                    if (y == cubeY) {
                        continue;
                    }
                    ((ICubicWorld) world).getCubeFromCubeCoords(cubeX, y, cubeZ);
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
                                if (y < Cube.SIZE / 2) {
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
    public void populate(ICube cube) {
        tryInit(vanilla, world);
        if (cube.getY() < 0 || cube.getY() >= worldHeightCubes) {
            return;
        }
        // Cubes outside this range are only filled with their respective block
        // No population takes place
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) {
            for (int y = worldHeightCubes - 1; y >= 0; y--) {
                // normal populators would not do this... but we are populating more than one cube!
                ((ICubicWorldInternal) world).getCubeFromCubeCoords(cube.getX(), y, cube.getZ()).setPopulated(true);
            }

            try {
                vanilla.populate(cube.getX(), cube.getZ());
            } catch (IllegalArgumentException ex) {
                StackTraceElement[] stack = ex.getStackTrace();
                if (stack == null || stack.length < 1 ||
                        !stack[0].getClassName().equals(Random.class.getName()) ||
                        !stack[0].getMethodName().equals("nextInt")) {
                    throw ex;
                } else {
                    CubicChunks.LOGGER.error("Error while populating. Likely known mod issue, ignoring...", ex);
                }
            }
            GameRegistry.generateWorld(cube.getX(), cube.getZ(), world, vanilla, world.getChunkProvider());
        }
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) {
            return new Box(
                    -1, -cube.getY(), -1,
                    0, worldHeightCubes - cube.getY() - 1, 0
            );
        }
        return NO_REQUIREMENT;
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) {
            return new Box(
                    0, -cube.getY(), 0,
                    1, worldHeightCubes - cube.getY() - 1, 1
            );
        }
        return NO_REQUIREMENT;
    }

    @Override
    public void recreateStructures(ICube cube) {
    }

    @Override
    public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return vanilla.getPossibleCreatures(creatureType, pos);
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        return vanilla.getNearestStructurePos((World) world, name, pos, findUnexplored);
    }
}
