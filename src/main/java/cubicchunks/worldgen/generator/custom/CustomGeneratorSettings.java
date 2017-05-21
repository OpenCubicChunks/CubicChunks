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
import cubicchunks.CubicChunks;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.biome.replacer.BiomeBlockReplacerConfig;
import net.minecraft.world.gen.ChunkProviderSettings;

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

    public int lavaLakeRarity = 80;
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

    public int dirtSpawnSize = 33;
    public int dirtSpawnTries = 10;
    public float dirtSpawnProbability = 1f / (256f / Cube.SIZE);
    public float dirtSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float dirtSpawnMaxHeight = Float.POSITIVE_INFINITY;

    public int gravelSpawnSize = 33;
    public int gravelSpawnTries = 8;
    public float gravelSpawnProbability = 1f / (256f / Cube.SIZE);
    public float gravelSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float gravelSpawnMaxHeight = Float.POSITIVE_INFINITY;

    public int graniteSpawnSize = 33;
    public int graniteSpawnTries = 10;
    public float graniteSpawnProbability = 256f / 80f / (256f / Cube.SIZE);
    public float graniteSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float graniteSpawnMaxHeight = (80f - 64f) / 64f;

    public int dioriteSpawnSize = 33;
    public int dioriteSpawnTries = 10;
    public float dioriteSpawnProbability = 256f / 80f / (256f / Cube.SIZE);
    public float dioriteSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float dioriteSpawnMaxHeight = (80f - 64f) / 64f;

    public int andesiteSpawnSize = 33;
    public int andesiteSpawnTries = 10;
    public float andesiteSpawnProbability = 256f / 80f / (256f / Cube.SIZE);
    public float andesiteSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float andesiteSpawnMaxHeight = (80f - 64f) / 64f;

    public int coalOreSpawnSize = 17;
    public int coalOreSpawnTries = 20;
    public float coalOreSpawnProbability = 256f / 128f / (256f / Cube.SIZE);
    public float coalOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float coalOreSpawnMaxHeight = 1;

    public int ironOreSpawnSize = 9;
    public int ironOreSpawnTries = 20;
    public float ironOreSpawnProbability = 256f / 64f / (256f / Cube.SIZE);
    public float ironOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float ironOreSpawnMaxHeight = 0;

    public int goldOreSpawnSize = 9;
    public int goldOreSpawnTries = 2;
    public float goldOreSpawnProbability = 256f / 32f / (256f / Cube.SIZE);
    public float goldOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float goldOreSpawnMaxHeight = -0.5f;

    public int redstoneOreSpawnSize = 8;
    public int redstoneOreSpawnTries = 8;
    public float redstoneOreSpawnProbability = 256f / 16f / (256f / Cube.SIZE);
    public float redstoneOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float redstoneOreSpawnMaxHeight = -0.75f;

    public int diamondOreSpawnSize = 8;
    public int diamondOreSpawnTries = 1;
    public float diamondOreSpawnProbability = 256f / 16f / (256f / Cube.SIZE);
    public float diamondOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float diamondOreSpawnMaxHeight = -0.75f;

    public int lapisLazuliSpawnSize = 7;
    public int lapisLazuliSpawnTries = 1;
    public float lapisLazuliSpawnProbability = 256f / 32f / (256f / Cube.SIZE);
    public float lapisLazuliHeightMean = 0.25f; // actually vanilla closest fit is 15/64 and 7/64
    public float lapisLazuliHeightStdDeviation = 0.125f;

    public int hillsEmeraldOreSpawnTries = 11; // actually there are on average 5.5 attempts per chunk, so multiply prob. by 0.5
    public float hillsEmeraldOreSpawnProbability = 0.5f * 256f / 28f / (256f / Cube.SIZE);
    public float hillsEmeraldOreSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float hillsEmeraldOreSpawnMaxHeight = -0.5f;

    public int hillsSilverfishStoneSpawnSize = 7;
    public int hillsSilverfishStoneSpawnTries = 7;
    public float hillsSilverfishStoneSpawnProbability = 256f / 64f / (256f / Cube.SIZE);
    public float hillsSilverfishStoneSpawnMinHeight = Float.NEGATIVE_INFINITY;
    public float hillsSilverfishStoneSpawnMaxHeight = 0;

    public int mesaAddedGoldOreSpawnSize = 20;
    public int mesaAddedGoldOreSpawnTries = 2;
    public float mesaAddedGoldOreSpawnProbability = 256f / 32f / (256f / Cube.SIZE);
    public float mesaAddedGoldOreSpawnMinHeight = -0.5f;
    public float mesaAddedGoldOreSpawnMaxHeight = 0.25f;

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
        conf.set(CubicChunks.MODID, "ocean_level", this.waterLevel);
        return conf;
    }

    public String toJson() {
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        return gson.toJson(this);
    }

    public static CustomGeneratorSettings fromJson(String json) {
        if (json.isEmpty()) {
            return defaults();
        }
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        return gson.fromJson(json, CustomGeneratorSettings.class);
    }

    public static CustomGeneratorSettings defaults() {
        return new CustomGeneratorSettings();
    }

    public static CustomGeneratorSettings fromVanilla(ChunkProviderSettings settings) {
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
}
