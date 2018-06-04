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
package cubicchunks.worldgen.generator.custom;

import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.worldgen.generator.custom.builder.IBuilder.NEGATIVE;
import static cubicchunks.worldgen.generator.custom.builder.IBuilder.POSITIVE;

import cubicchunks.CubicChunks;
import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.util.XYZMap;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubeGeneratorsRegistry;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.biome.replacer.IBiomeBlockReplacer;
import cubicchunks.worldgen.generator.custom.builder.BiomeSource;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;
import cubicchunks.worldgen.generator.custom.structure.CubicCaveGenerator;
import cubicchunks.worldgen.generator.custom.structure.CubicRavineGenerator;
import cubicchunks.worldgen.generator.custom.structure.CubicStructureGenerator;
import cubicchunks.worldgen.generator.custom.structure.feature.CubicFeatureGenerator;
import cubicchunks.worldgen.generator.custom.structure.feature.CubicStrongholdGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A terrain generator that supports infinite(*) worlds
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomTerrainGenerator extends BasicCubeGenerator {

    private static final int CACHE_SIZE_2D = 16 * 16;
    private static final int CACHE_SIZE_3D = 16 * 16 * 16;
    private static final ToIntFunction<Vec3i> HASH_2D = (v) -> v.getX() + v.getZ() * 5;
    private static final ToIntFunction<Vec3i> HASH_3D = (v) -> v.getX() + v.getZ() * 5 + v.getY() * 25;
    private final Map<CustomGeneratorSettings.IntAABB, CustomTerrainGenerator> areaGenerators = new HashMap<>();
    // Number of octaves for the noise function
    private IBuilder terrainBuilder;
    private final BiomeSource biomeSource;
    private final CustomGeneratorSettings conf;
    private final Map<Biome, ICubicPopulator> populators = new HashMap<>();

    //TODO: Implement more structures
    @Nonnull private CubicCaveGenerator caveGenerator = new CubicCaveGenerator();
    @Nonnull private CubicStructureGenerator ravineGenerator = new CubicRavineGenerator();
    @Nonnull private CubicFeatureGenerator strongholds;

    public CustomTerrainGenerator(ICubicWorld world, final long seed) {
        this(world, CustomGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions()), seed);
    }

    public CustomTerrainGenerator(ICubicWorld world, CustomGeneratorSettings settings, final long seed) {
        super(world);
        this.conf = settings;

        for (Biome biome : ForgeRegistries.BIOMES) {
            CubicBiome cubicBiome = CubicBiome.getCubic(biome);
            populators.put(biome, cubicBiome.getDecorator(conf));
        }

        this.strongholds = new CubicStrongholdGenerator(conf);

        this.biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), world.getBiomeProvider(), 2);
        initGenerator(seed);

        if (settings.cubeAreas != null) {
            for (CustomGeneratorSettings.IntAABB aabb : settings.cubeAreas.keySet()) {
                this.areaGenerators.put(aabb, new CustomTerrainGenerator(world, settings.cubeAreas.get(aabb), seed));
            }
        }
    }

    private void initGenerator(long seed) {
        Random rnd = new Random(seed);

        IBuilder selector = NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.selectorNoiseFrequencyX, conf.selectorNoiseFrequencyY, conf.selectorNoiseFrequencyZ)
                .octaves(conf.selectorNoiseOctaves)
                .create()
                .mul(conf.selectorNoiseFactor).add(conf.selectorNoiseOffset).clamp(0, 1);

        IBuilder low = NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.lowNoiseFrequencyX, conf.lowNoiseFrequencyY, conf.lowNoiseFrequencyZ)
                .octaves(conf.lowNoiseOctaves)
                .create()
                .mul(conf.lowNoiseFactor).add(conf.lowNoiseOffset);

        IBuilder high = NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.highNoiseFrequencyX, conf.highNoiseFrequencyY, conf.highNoiseFrequencyZ)
                .octaves(conf.highNoiseOctaves)
                .create()
                .mul(conf.highNoiseFactor).add(conf.highNoiseOffset);

        IBuilder randomHeight2d = NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.depthNoiseFrequencyX, 0, conf.depthNoiseFrequencyZ)
                .octaves(conf.depthNoiseOctaves)
                .create()
                .mul(conf.depthNoiseFactor).add(conf.depthNoiseOffset)
                .mulIf(NEGATIVE, -0.3).mul(3).sub(2).clamp(-2, 1)
                .divIf(NEGATIVE, 2 * 2 * 1.4).divIf(POSITIVE, 8)
                .mul(0.2 * 17 / 64.0)
                .cached2d(CACHE_SIZE_2D, HASH_2D);

        IBuilder height = ((IBuilder) biomeSource::getHeight)
                .mul(conf.heightFactor)
                .add(conf.heightOffset);

        double specialVariationFactor = conf.specialHeightVariationFactorBelowAverageY;
        IBuilder volatility = ((IBuilder) biomeSource::getVolatility)
                .mul((x, y, z) -> height.get(x, y, z) > y ? specialVariationFactor : 1)
                .mul(conf.heightVariationFactor)
                .add(conf.heightVariationOffset);

        this.terrainBuilder = selector
                .lerp(low, high).add(randomHeight2d).mul(volatility).add(height)
                .sub(volatility.signum().mul((x, y, z) -> y))
                .cached(CACHE_SIZE_3D, HASH_3D);
    }

    @Override public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        if (!areaGenerators.isEmpty()) {
            for (CustomGeneratorSettings.IntAABB aabb : areaGenerators.keySet()) {
                if (!aabb.contains(cubeX, cubeY, cubeZ)) {
                    continue;
                }
                return areaGenerators.get(aabb).generateCube(cubeX, cubeY, cubeZ);
            }
        }
        ICubePrimer primer = new CubePrimer();
        generate(primer, cubeX, cubeY, cubeZ);
        generateStructures(primer, new CubePos(cubeX, cubeY, cubeZ));
        return primer;
    }

    @Override public void populate(Cube cube) {
        if (!areaGenerators.isEmpty()) {
            for (CustomGeneratorSettings.IntAABB aabb : areaGenerators.keySet()) {
                if (!aabb.contains(cube.getX(), cube.getY(), cube.getZ())) {
                    continue;
                }
                areaGenerators.get(aabb).populate(cube);
                return;
            }
        }
        /**
         * If event is not canceled we will use default biome decorators and
         * cube populators from registry.
         **/
        if (!MinecraftForge.EVENT_BUS.post(new CubePopulatorEvent(world, cube))) {
            CubicBiome biome = CubicBiome.getCubic(cube.getCubicWorld().getBiome(Coords.getCubeCenter(cube)));

            CubePos pos = cube.getCoords();
            // For surface generators we should actually use special RNG with
            // seed
            // that depends only in world seed and cube X/Z
            // but using this for surface generation doesn't cause any
            // noticeable issues
            Random rand = new Random(cube.cubeRandomSeed());
            populators.get(biome.getBiome()).generate(world, rand, pos, biome);
            CubeGeneratorsRegistry.generateWorld(world, rand, pos, biome);

            strongholds.generateStructure((World) world, rand, pos);
        }
    }

    @Override public Box getPopulationRequirement(Cube cube) {
        return RECOMMENDED_POPULATOR_REQUIREMENT;
    }

    @Override
    public void recreateStructures(Cube cube) {
        this.strongholds.generate(world, null, cube.getCoords());
    }

    @Nullable @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        if ("Stronghold".equals(name)) {
            return strongholds.getClosestStrongholdPos((World) world, pos, true);
        }
        return null;
    }

    /**
     * Generate the cube as the specified location
     *
     * @param cubePrimer cube primer to use
     * @param cubeX cube x location
     * @param cubeY cube y location
     * @param cubeZ cube z location
     */
    private void generate(final ICubePrimer cubePrimer, int cubeX, int cubeY, int cubeZ) {
        // when debugging is enabled, allow reloading generator settings after pressing L
        // no need to restart after applying changes.
        // Seed it changed to some constant because world isn't easily accessible here
        if (CubicChunks.DEBUG_ENABLED && FMLCommonHandler.instance().getSide().isClient() && Keyboard.isKeyDown(Keyboard.KEY_L)) {
            initGenerator(42);
        }

        BlockPos start = new BlockPos(cubeX * 4, cubeY * 2, cubeZ * 4);
        BlockPos end = start.add(4, 2, 4);
        terrainBuilder.forEachScaled(start, end, new Vec3i(4, 8, 4),
                (x, y, z, dx, dy, dz, v) ->
                        cubePrimer.setBlockState(
                                blockToLocal(x), blockToLocal(y), blockToLocal(z),
                                getBlock(x, y, z, dx, dy, dz, v))
        );

    }

    /**
     * Retrieve the blockstate appropriate for the specified builder entry
     *
     * @return The block state
     */
    private IBlockState getBlock(int x, int y, int z, double dx, double dy, double dz, double density) {
        List<IBiomeBlockReplacer> replacers = biomeSource.getReplacers(x, y, z);
        IBlockState block = Blocks.AIR.getDefaultState();
        int size = replacers.size();
        for (int i = 0; i < size; i++) {
            block = replacers.get(i).getReplacedBlock(block, x, y, z, dx, dy, dz, density);
        }
        return block;
    }

    private void generateStructures(ICubePrimer cube, CubePos cubePos) {
        // generate world populator
        if (this.conf.caves) {
            this.caveGenerator.generate(world, cube, cubePos);
        }
        if (this.conf.ravines) {
            this.ravineGenerator.generate(world, cube, cubePos);
        }
        if (this.conf.strongholds) {
            this.strongholds.generate(world, cube, cubePos);
        }
    }
}
