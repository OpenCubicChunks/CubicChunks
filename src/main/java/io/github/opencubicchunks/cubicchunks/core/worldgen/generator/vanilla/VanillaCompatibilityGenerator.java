/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IGameRegistry;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.worldgen.WorldgenHangWatchdog;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

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
    private int worldHeightCubes;
    @Nonnull private final IChunkGenerator vanilla;
    @Nonnull private final World world;
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
    @Nonnull private IBlockState extensionBlockBottom = Blocks.STONE.getDefaultState();
    /**
     * Detected block for filling cubes above the world
     */
    @Nonnull private IBlockState extensionBlockTop = Blocks.AIR.getDefaultState();

    private boolean hasTopBedrock = false, hasBottomBedrock = true;

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

        int worldHeightBlocks = ((ICubicWorld) world).getMaxGenerationHeight();
        worldHeightCubes = worldHeightBlocks / Cube.SIZE;
        Map<IBlockState, Integer> blockHistogramBottom = new HashMap<>();
        Map<IBlockState, Integer> blockHistogramTop = new HashMap<>();

        ExtendedBlockStorage bottomEBS = lastChunk.getBlockStorageArray()[0];
        for (int x = 0; x < Cube.SIZE; x++) {
            for (int z = 0; z < Cube.SIZE; z++) {
                // Scan three layers top / bottom each to guard against bedrock walls
                for (int y = 0; y < 3; y++) {
                    IBlockState blockState = bottomEBS == null ?
                            Blocks.AIR.getDefaultState() : bottomEBS.get(x, y, z);

                    int count = blockHistogramBottom.getOrDefault(blockState, 0);
                    blockHistogramBottom.put(blockState, count + 1);
                }

                for (int y = worldHeightBlocks - 1; y > worldHeightBlocks - 4; y--) {
                    int localY = Coords.blockToLocal(y);
                    ExtendedBlockStorage ebs = lastChunk.getBlockStorageArray()[Coords.blockToCube(y)];

                    IBlockState blockState = ebs == null ? Blocks.AIR.getDefaultState() : ebs.get(x, localY, z);

                    int count = blockHistogramTop.getOrDefault(blockState, 0);
                    blockHistogramTop.put(blockState, count + 1);
                }
            }
        }

        CubicChunks.LOGGER.debug("Block histograms: \nTop: " + blockHistogramTop + "\nBottom: " + blockHistogramBottom);

        int topcount = 0;
        for (Map.Entry<IBlockState, Integer> entry : blockHistogramBottom.entrySet()) {
            if (entry.getValue() > topcount && entry.getKey().getBlock() != Blocks.BEDROCK) {
                extensionBlockBottom = entry.getKey();
                topcount = entry.getValue();
            }
        }
        hasBottomBedrock = blockHistogramBottom.getOrDefault(Blocks.BEDROCK.getDefaultState(), 0) > 0;
        CubicChunks.LOGGER.info("Detected filler block " + extensionBlockBottom.getBlock().getTranslationKey() + " " +
                "from layers [0, 2], bedrock=" + hasBottomBedrock);

        topcount = 0;
        for (Map.Entry<IBlockState, Integer> entry : blockHistogramTop.entrySet()) {
            if (entry.getValue() > topcount && entry.getKey().getBlock() != Blocks.BEDROCK) {
                extensionBlockTop = entry.getKey();
                topcount = entry.getValue();
            }
        }
        hasTopBedrock = blockHistogramTop.getOrDefault(Blocks.BEDROCK.getDefaultState(), 0) > 0;
        CubicChunks.LOGGER.info("Detected filler block " + extensionBlockTop.getBlock().getTranslationKey() + " from" +
                " layers [" + (worldHeightBlocks - 3) + ", " + (worldHeightBlocks - 1) + "], bedrock=" + hasTopBedrock);
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
    
    private Random getCubeSpecificRandom(int cubeX, int cubeY, int cubeZ) {
        Random rand = new Random(world.getSeed());
        rand.setSeed(rand.nextInt() ^ cubeX);
        rand.setSeed(rand.nextInt() ^ cubeZ);
        rand.setSeed(rand.nextInt() ^ cubeY);
        return rand;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        try {
            WorldgenHangWatchdog.startWorldGen();
            tryInit(vanilla, world);
            CubePrimer primer = new CubePrimer();

            Random rand = new Random(world.getSeed());
            rand.setSeed(rand.nextInt() ^ cubeX);
            rand.setSeed(rand.nextInt() ^ cubeZ);
            if (cubeY < 0 || cubeY >= worldHeightCubes) {
                // Fill with bottom block
                for (int y = 0; y < Cube.SIZE; y++) {
                    for (int z = 0; z < Cube.SIZE; z++) {
                        for (int x = 0; x < Cube.SIZE; x++) {
                            IBlockState state = cubeY < 0 ? extensionBlockBottom : extensionBlockTop;
                            int blockY = Coords.localToBlock(cubeY, y);
                            state = WorldGenUtils.getRandomBedrockReplacement(world, rand, state, blockY, 5,
                                    hasTopBedrock, hasBottomBedrock);
                            primer.setBlockState(x, y, z, state);
                        }
                    }
                }
            } else {
                // Make vanilla generate a chunk for us to copy
                if (lastChunk.x != cubeX || lastChunk.z != cubeZ) {
                    if (CubicChunksConfig.optimizedCompatibilityGenerator) {
                        try (ICubicWorldInternal.CompatGenerationScope ignored =
                                     ((ICubicWorldInternal.Server) world).doCompatibilityGeneration()) {
                            lastChunk = vanilla.generateChunk(cubeX, cubeZ);
                            ChunkPrimer chunkPrimer = ((IColumnInternal) lastChunk).getCompatGenerationPrimer();
                            if (chunkPrimer == null) {
                                CubicChunks.LOGGER.error("Optimized compatibility generation failed, disabling...");
                                CubicChunksConfig.optimizedCompatibilityGenerator = false;
                            } else {
                                replaceBedrock(chunkPrimer, rand);
                            }
                        }
                    } else {
                        lastChunk = vanilla.generateChunk(cubeX, cubeZ);
                    }
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
                ChunkPrimer chunkPrimer = ((IColumnInternal) lastChunk).getCompatGenerationPrimer();
                if (chunkPrimer != null) {
                    return new CubePrimerWrapper(chunkPrimer, cubeY);
                }
                ExtendedBlockStorage storage = lastChunk.getBlockStorageArray()[cubeY];
                if (((ICubicWorld) world).getMaxHeight() == 16) {
                    if (cubeY != 0) {
                        storage = null;
                    } else {
                        storage = lastChunk.getBlockStorageArray()[4];
                    }
                }
                if (storage != null && !storage.isEmpty()) {
                    for (int y = 0; y < Cube.SIZE; y++) {
                        int blockY = Coords.localToBlock(cubeY, y);
                        for (int z = 0; z < Cube.SIZE; z++) {
                            for (int x = 0; x < Cube.SIZE; x++) {
                                IBlockState state = storage.get(x, y, z);
                                if (state == Blocks.BEDROCK.getDefaultState()) {
                                    if (y < Cube.SIZE / 2) {
                                        state = extensionBlockBottom;
                                    } else {
                                        state = extensionBlockTop;
                                    }
                                    state = WorldGenUtils.getRandomBedrockReplacement(world, rand, state, blockY, 5,
                                            hasTopBedrock, hasBottomBedrock);
                                    primer.setBlockState(x, y, z, state);
                                } else {
                                    state = WorldGenUtils.getRandomBedrockReplacement(world, rand, state, blockY, 5,
                                            hasTopBedrock, hasBottomBedrock);
                                    primer.setBlockState(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }

            return primer;
        } finally {
            WorldgenHangWatchdog.endWorldGen();
        }
    }

    private void replaceBedrock(ChunkPrimer chunkPrimer, Random rand) {
        for (int y = 0; y < 8; y++) {
            replaceBedrockAtLayer(chunkPrimer, rand, y);
        }
        int startY = Coords.localToBlock(worldHeightCubes - 1, 8);
        int endY = Coords.cubeToMinBlock(worldHeightCubes);
        for (int y = startY; y < endY; y++) {
            replaceBedrockAtLayer(chunkPrimer, rand, y);
        }
    }

    private void replaceBedrockAtLayer(ChunkPrimer chunkPrimer, Random rand, int y) {
        for (int z = 0; z < Cube.SIZE; z++) {
            for (int x = 0; x < Cube.SIZE; x++) {
                IBlockState state = chunkPrimer.getBlockState(x, y, z);
                if (state == Blocks.BEDROCK.getDefaultState()) {
                    if (y < 64) {
                        chunkPrimer.setBlockState(x, y, z,
                                WorldGenUtils.getRandomBedrockReplacement(world, rand, extensionBlockBottom, y, 5, hasTopBedrock, hasBottomBedrock));
                    } else {
                        chunkPrimer.setBlockState(x, y, z,
                                WorldGenUtils.getRandomBedrockReplacement(world, rand, extensionBlockTop, y, 5, hasTopBedrock, hasBottomBedrock));
                    }
                }
            }
        }
    }

    @Override
    public void populate(ICube cube) {
        try {
            WorldgenHangWatchdog.startWorldGen();
            tryInit(vanilla, world);
            Random rand = getCubeSpecificRandom(cube.getX(), cube.getY(), cube.getZ());
            CubeGeneratorsRegistry.populateVanillaCubic(world, rand, cube);
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
                applyModGenerators(cube.getX(), cube.getZ(), world, vanilla, world.getChunkProvider());
            }
        } finally {
            WorldgenHangWatchdog.endWorldGen();
        }
    }

    private void applyModGenerators(int x, int z, World world, IChunkGenerator vanillaGen, IChunkProvider provider) {
        List<IWorldGenerator> generators = IGameRegistry.getSortedGeneratorList();
        if (generators == null) {
            IGameRegistry.computeGenerators();
            generators = IGameRegistry.getSortedGeneratorList();
            assert generators != null;
        }
        long worldSeed = world.getSeed();
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * x + zSeed * z) ^ worldSeed;

        for (IWorldGenerator generator : generators) {
            fmlRandom.setSeed(chunkSeed);
            try {
                CompatHandler.beforeGenerate(world, generator);
                generator.generate(fmlRandom, x, z, world, vanillaGen, provider);
            } finally {
                CompatHandler.afterGenerate(world);
            }
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
        return vanilla.getNearestStructurePos(world, name, pos, findUnexplored);
    }

    private static class CubePrimerWrapper extends CubePrimer {
        private final ChunkPrimer chunkPrimer;
        private final int cubeYBase;

        public CubePrimerWrapper(ChunkPrimer chunkPrimer, int cubeY) {
            super(null);
            this.chunkPrimer = chunkPrimer;
            this.cubeYBase = Coords.cubeToMinBlock(cubeY);
        }

        @Override
        public IBlockState getBlockState(int x, int y, int z) {
            return chunkPrimer.getBlockState(x, y | cubeYBase, z);
        }

        @Override
        public void setBlockState(int x, int y, int z, @Nonnull IBlockState state) {
            chunkPrimer.setBlockState(x, y | cubeYBase, z, state);
        }
    }
}
