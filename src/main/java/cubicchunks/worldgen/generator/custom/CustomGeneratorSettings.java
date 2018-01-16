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

import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FREQUENCY;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_LOWHIGH_NOISE_FREQUENCY_Y;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_XZ;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_Y;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_OFFSET;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.frequencyFromVanilla;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import cubicchunks.CCFixType;
import cubicchunks.CubicChunks;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.biome.replacer.BiomeBlockReplacerConfig;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class CustomGeneratorSettings {
    /**
     * Note: many of these values are unused yet
     */

    /**
     * Vanilla standard options
     * <p>
     * Page 1
     */
    public int waterLevel = 63;
    public boolean caves = true;

    public boolean strongholds = true;
    public boolean alternateStrongholdsPositions = false; // TODO: add to gui
    public boolean villages = true;

    public boolean mineshafts = true;
    public boolean temples = true;

    public boolean oceanMonuments = true;
    public boolean woodlandMansions = true;

    public boolean ravines = true;
    public boolean dungeons = true;

    public int dungeonCount = 7;
    public boolean waterLakes = true;

    public int waterLakeRarity = 4;
    public boolean lavaLakes = true;

    public int lavaLakeRarity = 8;
    public int aboveSeaLavaLakeRarity = 13; // approximately 10 * 4/3, all that end up above the surface are at the surface in vanilla
    public boolean lavaOceans = false;

    public int biome = -1;
    public int biomeSize = 4;
    public int riverSize = 4;

    /**
     * Vanilla standard options
     * <p>
     * Page 2
     */

    // probability: (vanillaChunkHeight/oreGenRangeSize) / amountOfCubesInVanillaChunk

    public List<StandardOreConfig> standardOres = new ArrayList<>();

    public List<PeriodicGaussianOreConfig> periodicGaussianOres = new ArrayList<>();

    /**
     * Terrain shape
     */

    public float heightVariationFactor = 64;
    public float specialHeightVariationFactorBelowAverageY = 0.25f;
    public float heightVariationOffset = 0;
    public float heightFactor = 64;// height scale
    public float heightOffset = 64;// sea level

    public float depthNoiseFactor = VANILLA_DEPTH_NOISE_FACTOR;
    public float depthNoiseOffset = 0;
    public float depthNoiseFrequencyX = VANILLA_DEPTH_NOISE_FREQUENCY;
    public float depthNoiseFrequencyZ = VANILLA_DEPTH_NOISE_FREQUENCY;
    public int depthNoiseOctaves = 16;

    public float selectorNoiseFactor = VANILLA_SELECTOR_NOISE_FACTOR;
    public float selectorNoiseOffset = VANILLA_SELECTOR_NOISE_OFFSET;
    public float selectorNoiseFrequencyX = VANILLA_SELECTOR_NOISE_FREQUENCY_XZ;
    public float selectorNoiseFrequencyY = VANILLA_SELECTOR_NOISE_FREQUENCY_Y;
    public float selectorNoiseFrequencyZ = VANILLA_SELECTOR_NOISE_FREQUENCY_XZ;
    public int selectorNoiseOctaves = 8;

    public float lowNoiseFactor = 1;
    public float lowNoiseOffset = 0;
    public float lowNoiseFrequencyX = VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
    public float lowNoiseFrequencyY = VANILLA_LOWHIGH_NOISE_FREQUENCY_Y;
    public float lowNoiseFrequencyZ = VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
    public int lowNoiseOctaves = 16;

    public float highNoiseFactor = 1;
    public float highNoiseOffset = 0;
    public float highNoiseFrequencyX = VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
    public float highNoiseFrequencyY = VANILLA_LOWHIGH_NOISE_FREQUENCY_Y;
    public float highNoiseFrequencyZ = VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
    public int highNoiseOctaves = 16;

    // TODO: public boolean negativeHeightVariationInvertsTerrain = true;

    public CustomGeneratorSettings() {

    }

    public BiomeBlockReplacerConfig createBiomeBlockReplacerConfig() {
        BiomeBlockReplacerConfig conf = new BiomeBlockReplacerConfig();
        conf.fillDefaults();
        conf.set(CubicChunks.MODID, "water_level", (double) this.waterLevel);
        conf.set(CubicChunks.MODID, "height_scale", (double) this.heightFactor);
        conf.set(CubicChunks.MODID, "height_offset", (double) this.heightOffset);
        return conf;
    }

    public String toJson() {
        Gson gson = gson();
        return gson.toJson(this);
    }

    public static CustomGeneratorSettings fromJson(String json) {
        if (json.isEmpty()) {
            return defaults();
        }
        Gson gson = gson();
        return gson.fromJson(json, CustomGeneratorSettings.class);
    }

    public static CustomGeneratorSettings defaults() {
        CustomGeneratorSettings settings = new CustomGeneratorSettings();
        {
            settings.standardOres.addAll(Arrays.asList(
                    StandardOreConfig.builder()
                            .block(Blocks.DIRT.getDefaultState())
                            .size(33).attempts(10).probability(1f / (256f / Cube.SIZE)).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.GRAVEL.getDefaultState())
                            .size(33).attempts(8).probability(1f / (256f / Cube.SIZE)).create(),

                    StandardOreConfig.builder()
                            .block(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE))
                            .size(33).attempts(10).probability(256f / 80f / (256f / Cube.SIZE))
                            .maxHeight((80f - 64f) / 64f).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE))
                            .size(33).attempts(10).probability(256f / 80f / (256f / Cube.SIZE))
                            .maxHeight((80f - 64f) / 64f).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE))
                            .size(33).attempts(10).probability(256f / 80f / (256f / Cube.SIZE))
                            .maxHeight((80f - 64f) / 64f).create(),

                    StandardOreConfig.builder()
                            .block(Blocks.COAL_ORE.getDefaultState())
                            .size(17).attempts(20).probability(256f / 128f / (256f / Cube.SIZE))
                            .maxHeight(1).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.IRON_ORE.getDefaultState())
                            .size(33).attempts(10).probability(256f / 64f / (256f / Cube.SIZE))
                            .maxHeight(0).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.GOLD_ORE.getDefaultState())
                            .size(9).attempts(2).probability(256f / 32f / (256f / Cube.SIZE))
                            .maxHeight(-0.5f).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.REDSTONE_ORE.getDefaultState())
                            .size(8).attempts(8).probability(256f / 16f / (256f / Cube.SIZE))
                            .maxHeight(-0.75f).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.DIAMOND_ORE.getDefaultState())
                            .size(8).attempts(1).probability(256f / 16f / (256f / Cube.SIZE))
                            .maxHeight(-0.75f).create(),

                    StandardOreConfig.builder()
                            .block(Blocks.EMERALD_ORE.getDefaultState())
                            .size(1).attempts(11).probability(0.5f * 256f / 28f / (256f / Cube.SIZE))
                            .maxHeight(0)
                            .biomes(Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES, Biomes.MUTATED_EXTREME_HILLS,
                                    Biomes.MUTATED_EXTREME_HILLS_WITH_TREES).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE))
                            .size(7).attempts(7).probability(256f / 64f / (256f / Cube.SIZE))
                            .maxHeight(-0.5f)
                            .biomes(Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES, Biomes.MUTATED_EXTREME_HILLS,
                                    Biomes.MUTATED_EXTREME_HILLS_WITH_TREES).create(),
                    StandardOreConfig.builder()
                            .block(Blocks.GOLD_ORE.getDefaultState())
                            .size(20).attempts(2).probability(256f / 32f / (256f / Cube.SIZE))
                            .minHeight(-0.5f).maxHeight(0.25f)
                            .biomes(Biomes.MESA, Biomes.MESA_CLEAR_ROCK, Biomes.MESA_ROCK, Biomes.MUTATED_MESA, Biomes.MUTATED_MESA_CLEAR_ROCK,
                                    Biomes.MUTATED_MESA_ROCK).create()
            ));
        }

        {
            settings.periodicGaussianOres.addAll(Arrays.asList(
                    PeriodicGaussianOreConfig.builder()
                            .block(Blocks.LAPIS_ORE.getDefaultState())
                            .size(7).attempts(1).probability(0.933307775f) //resulted by approximating triangular behaviour with bell curve
                            .heightMean(-0.75f/*first belt at=16*/).heightStdDeviation(0.11231704455f/*x64 = 7.1882908513*/)
                            .heightSpacing(3.0f/*192*/)
                            .maxHeight(-0.5f).create()
            ));
        }
        return settings;
    }

    public static CustomGeneratorSettings fromVanilla(ChunkGeneratorSettings settings) {
        CustomGeneratorSettings obj = defaults();

        obj.lowNoiseFactor = 512.0f / settings.lowerLimitScale;
        obj.highNoiseFactor = 512.0f / settings.upperLimitScale;

        obj.depthNoiseFrequencyX = frequencyFromVanilla(settings.depthNoiseScaleX, 16);
        obj.depthNoiseFrequencyZ = frequencyFromVanilla(settings.depthNoiseScaleZ, 16);
        // settings.depthNoiseScaleExponent is ignored by vanilla

        obj.selectorNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale / settings.mainNoiseScaleX, 8);
        obj.selectorNoiseFrequencyY = frequencyFromVanilla(settings.heightScale / settings.mainNoiseScaleY, 8);
        obj.selectorNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale / settings.mainNoiseScaleZ, 8);

        obj.lowNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale, 16);
        obj.lowNoiseFrequencyY = frequencyFromVanilla(settings.heightScale, 16);
        obj.lowNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale, 16);

        obj.highNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale, 16);
        obj.highNoiseFrequencyY = frequencyFromVanilla(settings.heightScale, 16);
        obj.highNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale, 16);

        return obj;
    }

    public int getMinHeight() {
        return (int) (this.heightOffset - getMaxHeightOffset());
    }

    public int getMaxHeight() {
        return (int) (this.heightOffset + getMaxHeightOffset());
    }

    public int getAverageHeight() {
        return (int) this.heightOffset;
    }

    private int getMaxHeightOffset() {
        return (int) Math.max(heightFactor, Math.abs(heightVariationFactor) + Math.abs(heightVariationOffset));
    }

    public int getRealMaxHeight() {
        return (int) (this.heightOffset + heightVariationOffset +
                Math.max(this.heightFactor * 2 + this.heightVariationFactor, this.heightFactor + this.heightVariationFactor * 2));
    }

    public static void registerDataFixers(ModFixs fixes) {
        fixes.registerFix(CCFixType.forWorldType("CustomCubic"), new IFixableData() {
            @Override public int getFixVersion() {
                return -1;
            }

            @Override public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
                String generatorOptions = compound.getString("generatorOptions");
                Gson gson = gson();

                JsonReader reader = new JsonReader(new StringReader(generatorOptions));
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

                JsonArray standardOres = new JsonArray();
                JsonArray periodicGaussianOres = new JsonArray();

                // kind of ugly but I don'twant to make a special class just so store these 3 objects...
                String[] standard = {
                        "dirt",
                        "gravel",
                        "granite",
                        "diorite",
                        "andesite",
                        "coalOre",
                        "ironOre",
                        "goldOre",
                        "redstoneOre",
                        "diamondOre",
                        "hillsEmeraldOre",
                        "hillsSilverfishStone",
                        "mesaAddedGoldOre"
                };
                IBlockState[] standardBlockstates = {
                        Blocks.DIRT.getDefaultState(),
                        Blocks.GRAVEL.getDefaultState(),
                        Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE),
                        Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE),
                        Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE),
                        Blocks.COAL_ORE.getDefaultState(),
                        Blocks.IRON_ORE.getDefaultState(),
                        Blocks.GOLD_ORE.getDefaultState(),
                        Blocks.REDSTONE_ORE.getDefaultState(),
                        Blocks.DIAMOND_ORE.getDefaultState(),
                        Blocks.EMERALD_ORE.getDefaultState(),
                        Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE),
                        Blocks.GOLD_ORE.getDefaultState()
                };
                Biome[][] standardBiomes = {
                        null, // dirt
                        null, // gravel
                        null, // granite
                        null, // diorite
                        null, // andesite
                        null, // coal
                        null, // iron
                        null, // gold
                        null, // redstone
                        null, // diamond
                        {Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES, Biomes.MUTATED_EXTREME_HILLS,
                                Biomes.MUTATED_EXTREME_HILLS_WITH_TREES},//emerald
                        {Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES, Biomes.MUTATED_EXTREME_HILLS,
                                Biomes.MUTATED_EXTREME_HILLS_WITH_TREES},//monster egg
                        {Biomes.MESA, Biomes.MESA_CLEAR_ROCK, Biomes.MESA_ROCK, Biomes.MUTATED_MESA, Biomes.MUTATED_MESA_CLEAR_ROCK,
                                Biomes.MUTATED_MESA_ROCK},//mesa gold
                };
                for (int i = 0; i < standard.length; i++) {
                    standardOres.add(convertStandardOre(gson, root, standard[i], standardBlockstates[i], standardBiomes[i]));
                }
                periodicGaussianOres.add(convertGaussianPeriodicOre(gson, root, "lapisLazuli", Blocks.LAPIS_ORE.getDefaultState(), null));
                root.add("standardOres", standardOres);
                root.add("periodicGaussianOres", periodicGaussianOres);
                compound.setString("generatorOptions", gson.toJson(root));
                return compound;
            }

            private JsonObject convertStandardOre(Gson gson, JsonObject root, String ore, IBlockState state, Biome[] biomes) {
                JsonObject obj = new JsonObject();
                obj.add("blockstate", gson.toJsonTree(state));
                if (biomes != null) {
                    obj.add("biomes", gson.toJsonTree(biomes));
                }
                obj.add("spawnSize", root.remove(ore + "SpawnSize"));
                obj.add("spawnTries", root.remove(ore + "SpawnTries"));
                obj.add("spawnProbability", root.remove(ore + "SpawnProbability"));
                obj.add("minHeight", root.remove(ore + "SpawnMinHeight"));
                obj.add("maxHeight", root.remove(ore + "SpawnMaxHeight"));
                return obj;
            }

            private JsonObject convertGaussianPeriodicOre(Gson gson, JsonObject root, String ore, IBlockState state, Biome[] biomes) {
                JsonObject obj = convertStandardOre(gson, root, ore, state, biomes);
                obj.add("heightMean", root.remove(ore + "HeightMean"));
                obj.add("heightStdDeviation", root.remove(ore + "HeightStdDeviation"));
                obj.add("heightSpacing", root.remove(ore + "HeightSpacing"));
                return obj;
            }
        });
    }

    public static Gson gson() {
        return new GsonBuilder().serializeSpecialFloatingPointValues()
                .registerTypeHierarchyAdapter(IBlockState.class, new BlockStateSerializer())
                .registerTypeHierarchyAdapter(Biome.class, new BiomeSerializer())
                .create();
    }

    public static class StandardOreConfig {

        public IBlockState blockstate;
        // null == no biome restrictions
        public Set<Biome> biomes;
        public int spawnSize;
        public int spawnTries;
        public float spawnProbability = 1.0f;
        public float minHeight = Float.NEGATIVE_INFINITY;
        public float maxHeight = Float.POSITIVE_INFINITY;

        private StandardOreConfig(IBlockState state, Set<Biome> biomes, int spawnSize, int spawnTries, float spawnProbability, float minHeight,
                float maxHeight) {
            this.blockstate = state;
            this.biomes = biomes;
            this.spawnSize = spawnSize;
            this.spawnTries = spawnTries;
            this.spawnProbability = spawnProbability;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private IBlockState blockstate;
            private Set<Biome> biomes = null;
            private int spawnSize;
            private int spawnTries;
            private float spawnProbability;
            private float minHeight = Float.NEGATIVE_INFINITY;
            private float maxHeight = Float.POSITIVE_INFINITY;

            public Builder block(IBlockState blockstate) {
                this.blockstate = blockstate;
                return this;
            }

            public Builder size(int spawnSize) {
                this.spawnSize = spawnSize;
                return this;
            }

            public Builder attempts(int spawnTries) {
                this.spawnTries = spawnTries;
                return this;
            }

            public Builder probability(float spawnProbability) {
                this.spawnProbability = spawnProbability;
                return this;
            }

            public Builder minHeight(float minHeight) {
                this.minHeight = minHeight;
                return this;
            }

            public Builder maxHeight(float maxHeight) {
                this.maxHeight = maxHeight;
                return this;
            }

            /**
             * If biomes is non-null, adds the biomes to allowed biomes, if it's null - removes biome-specific generation.
             */
            public Builder biomes(@Nullable Biome... biomes) {
                if (biomes == null) {
                    this.biomes = null;
                    return this;
                }
                if (this.biomes == null) {
                    this.biomes = new HashSet<>();
                }
                this.biomes.addAll(Arrays.asList(biomes));
                return this;
            }

            public Builder fromPeriodic(PeriodicGaussianOreConfig config) {
                return minHeight(config.minHeight)
                        .maxHeight(config.maxHeight)
                        .probability(config.spawnProbability)
                        .size(config.spawnSize)
                        .attempts(config.spawnTries)
                        .block(config.blockstate)
                        .biomes(config.biomes == null ? null : config.biomes.toArray(new Biome[0]));
            }
            public StandardOreConfig create() {
                return new StandardOreConfig(blockstate, biomes, spawnSize, spawnTries, spawnProbability, minHeight, maxHeight);
            }
        }
    }

    public static class PeriodicGaussianOreConfig {

        public IBlockState blockstate;
        public Set<Biome> biomes = null;
        public int spawnSize;
        public int spawnTries;
        public float spawnProbability;
        public float heightMean;
        public float heightStdDeviation;
        public float heightSpacing;
        public float minHeight;
        public float maxHeight;

        private PeriodicGaussianOreConfig(IBlockState blockstate, Set<Biome> biomes, int spawnSize, int spawnTries, float spawnProbability,
                float heightMean, float heightStdDeviation, float heightSpacing, float minHeight, float maxHeight) {
            this.blockstate = blockstate;
            this.biomes = biomes;
            this.spawnSize = spawnSize;
            this.spawnTries = spawnTries;
            this.spawnProbability = spawnProbability;
            this.heightMean = heightMean;
            this.heightStdDeviation = heightStdDeviation;
            this.heightSpacing = heightSpacing;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private IBlockState blockstate;
            private Set<Biome> biomes = null;
            private int spawnSize;
            private int spawnTries;
            private float spawnProbability;
            private float heightMean;
            private float heightStdDeviation;
            private float heightSpacing;
            private float minHeight = Float.NEGATIVE_INFINITY;
            private float maxHeight = Float.POSITIVE_INFINITY;

            public Builder block(IBlockState blockstate) {
                this.blockstate = blockstate;
                return this;
            }

            public Builder size(int spawnSize) {
                this.spawnSize = spawnSize;
                return this;
            }

            public Builder attempts(int spawnTries) {
                this.spawnTries = spawnTries;
                return this;
            }

            public Builder probability(float spawnProbability) {
                this.spawnProbability = spawnProbability;
                return this;
            }

            public Builder heightMean(float heightMean) {
                this.heightMean = heightMean;
                return this;
            }

            public Builder heightStdDeviation(float heightStdDeviation) {
                this.heightStdDeviation = heightStdDeviation;
                return this;
            }

            public Builder heightSpacing(float heightSpacing) {
                this.heightSpacing = heightSpacing;
                return this;
            }

            public Builder minHeight(float minHeight) {
                this.minHeight = minHeight;
                return this;
            }

            public Builder maxHeight(float maxHeight) {
                this.maxHeight = maxHeight;
                return this;
            }

            public Builder biomes(Biome... biomes) {
                if (biomes == null) {
                    this.biomes = null;
                    return this;
                }
                if (this.biomes == null) {
                    this.biomes = new HashSet<>();
                }
                this.biomes.addAll(Arrays.asList(biomes));
                return this;
            }

            public Builder fromStandard(StandardOreConfig config) {
                return minHeight(config.minHeight)
                        .maxHeight(config.maxHeight)
                        .probability(config.spawnProbability)
                        .size(config.spawnSize)
                        .attempts(config.spawnTries)
                        .block(config.blockstate)
                        .biomes(config.biomes == null ? null : config.biomes.toArray(new Biome[0]))
                        .heightMean(0)
                        .heightStdDeviation(1)
                        .heightSpacing(2);
            }

            public PeriodicGaussianOreConfig create() {
                return new PeriodicGaussianOreConfig(blockstate, biomes, spawnSize, spawnTries, spawnProbability, heightMean, heightStdDeviation,
                        heightSpacing, minHeight, maxHeight);
            }

        }
    }

    private static class BlockStateSerializer implements JsonDeserializer<IBlockState>, JsonSerializer<IBlockState> {

        public static final BlockStateSerializer INSTANCE = new BlockStateSerializer();

        @Override public IBlockState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String jsonString = json.toString();
            NBTTagCompound tag;
            try {
                tag = JsonToNBT.getTagFromJson(jsonString);
            } catch (NBTException e) {
                throw new JsonSyntaxException(e);
            }
            return NBTUtil.readBlockState(tag);
        }

        @Override public JsonElement serialize(IBlockState src, Type typeOfSrc, JsonSerializationContext context) {
            NBTTagCompound tag = new NBTTagCompound();
            NBTUtil.writeBlockState(tag, src);
            String tagString = tag.toString();
            return new JsonParser().parse(tagString);
        }
    }

    private static class BiomeSerializer implements JsonDeserializer<Biome>, JsonSerializer<Biome> {

        @Override public Biome deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ForgeRegistries.BIOMES.getValue(new ResourceLocation(json.getAsString()));
        }

        @Override public JsonElement serialize(Biome src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getRegistryName().toString());
        }
    }
}
