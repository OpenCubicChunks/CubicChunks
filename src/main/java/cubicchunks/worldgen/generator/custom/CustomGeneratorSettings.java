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

import net.minecraft.world.gen.ChunkProviderSettings;

import cubicchunks.CubicChunks;
import cubicchunks.worldgen.generator.custom.biome.replacer.BiomeBlockReplacerConfig;

import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FREQUENCY;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_LOWHIGH_NOISE_FREQUENCY_Y;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_XZ;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_Y;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_OFFSET;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.frequencyFromVanilla;

public class CustomGeneratorSettings {
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

	/**
	 * Block placement, pre-populator
	 */

	public int waterLevel = 63; // note: this is not named seaLevel to avoid confusion

	public CustomGeneratorSettings() {

	}

	public BiomeBlockReplacerConfig createBiomeBlockReplacerConfig() {
		BiomeBlockReplacerConfig conf = new BiomeBlockReplacerConfig();
		conf.fillDefaults();
		conf.set(CubicChunks.MODID, "ocean_level", this.waterLevel);
		return conf;
	}

	public static CustomGeneratorSettings defaults() {
		return new CustomGeneratorSettings();
	}

	public static CustomGeneratorSettings fromVanilla(ChunkProviderSettings settings) {
		CustomGeneratorSettings obj = defaults();

		obj.lowNoiseFactor = 512.0f/settings.lowerLimitScale;
		obj.highNoiseFactor = 512.0f/settings.upperLimitScale;

		obj.depthNoiseFrequencyX = frequencyFromVanilla(settings.depthNoiseScaleX, 16);
		obj.depthNoiseFrequencyZ = frequencyFromVanilla(settings.depthNoiseScaleZ, 16);
		// settings.depthNoiseScaleExponent is ignored by vanilla

		obj.selectorNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale/settings.mainNoiseScaleX, 8);
		obj.selectorNoiseFrequencyY = frequencyFromVanilla(settings.heightScale/settings.mainNoiseScaleY, 8);
		obj.selectorNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale/settings.mainNoiseScaleZ, 8);

		obj.lowNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale, 16);
		obj.lowNoiseFrequencyY = frequencyFromVanilla(settings.heightScale, 16);
		obj.lowNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale, 16);

		obj.highNoiseFrequencyX = frequencyFromVanilla(settings.coordinateScale, 16);
		obj.highNoiseFrequencyY = frequencyFromVanilla(settings.heightScale, 16);
		obj.highNoiseFrequencyZ = frequencyFromVanilla(settings.coordinateScale, 16);

		return obj;
	}
}
