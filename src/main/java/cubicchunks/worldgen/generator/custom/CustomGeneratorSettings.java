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

import cubicchunks.worldgen.generator.config.JsonConfig;
import cubicchunks.worldgen.generator.config.JsonConfigInitializer;
import cubicchunks.worldgen.generator.config.Value;

import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_DEPTH_NOISE_FREQUENCY;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_LOWHIGH_NOISE_FREQUENCY;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FACTOR;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_XZ;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_FREQUENCY_Y;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.VANILLA_SELECTOR_NOISE_OFFSET;
import static cubicchunks.worldgen.generator.custom.ConversionUtils.frequencyFromVanilla;

@JsonConfig
public class CustomGeneratorSettings {
	@Value(floatValue = 1) public float heightVariationFactor;
	@Value(floatValue = 0) public float heightVariationOffset;
	@Value(floatValue = 1) public float heightFactor;
	@Value(floatValue = 0) public float heightOffset;

	@Value(floatValue = VANILLA_DEPTH_NOISE_FACTOR) public float depthNoiseFactor;
	@Value(floatValue = 0) public float depthNoiseOffset;
	@Value(floatValue = VANILLA_DEPTH_NOISE_FREQUENCY) public float depthNoiseFrequencyX;
	@Value(floatValue = VANILLA_DEPTH_NOISE_FREQUENCY) public float depthNoiseFrequencyZ;
	@Value(intValue = 16) public int depthNoiseOctaves;

	@Value(floatValue = VANILLA_SELECTOR_NOISE_FACTOR) public float selectorNoiseFactor;
	@Value(floatValue = VANILLA_SELECTOR_NOISE_OFFSET) public float selectorNoiseOffset;
	@Value(floatValue = VANILLA_SELECTOR_NOISE_FREQUENCY_XZ) public float selectorNoiseFrequencyX;
	@Value(floatValue = VANILLA_SELECTOR_NOISE_FREQUENCY_Y) public float selectorNoiseFrequencyY;
	@Value(floatValue = VANILLA_SELECTOR_NOISE_FREQUENCY_XZ) public float selectorNoiseFrequencyZ;
	@Value(intValue = 8) public int selectorNoiseOctaves;

	@Value(floatValue = 1) public float lowNoiseFactor;
	@Value(floatValue = 0) public float lowNoiseOffset;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float lowNoiseFrequencyX;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float lowNoiseFrequencyY;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float lowNoiseFrequencyZ;
	@Value(intValue = 16) public int lowNoiseOctaves;

	@Value(floatValue = 1) public float highNoiseFactor;
	@Value(floatValue = 0) public float highNoiseOffset;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float highNoiseFrequencyX;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float highNoiseFrequencyY;
	@Value(floatValue = VANILLA_LOWHIGH_NOISE_FREQUENCY) public float highNoiseFrequencyZ;
	@Value(intValue = 16) public int highNoiseOctaves;

	public CustomGeneratorSettings() {
	}

	public static CustomGeneratorSettings defaults() {
		return new JsonConfigInitializer<>(CustomGeneratorSettings.class).defaults().build();
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
