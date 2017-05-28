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

import com.flowpowered.noise.Utils;
import net.minecraft.world.gen.NoiseGeneratorImproved;

import java.util.Random;

/**
 * Util class to convert from vanilla configs to cubic chunks config
 */
public class ConversionUtils {

    // uh... don't try to check it. It will take forever. It's correct. At least in MC 1.11.
    // --> /8 because we are using world coords instead of unscaled coords
    private static final float YSCALE = 8;
    private static final float XZSCALE = 4;

    public static final float VANILLA_DEPTH_NOISE_FACTOR = 1024f / 125.0f / YSCALE;
    public static final float VANILLA_SELECTOR_NOISE_FACTOR = 12.75f;
    public static final float VANILLA_SELECTOR_NOISE_OFFSET = 0.5f;

    public static final float VANILLA_DEPTH_NOISE_FREQUENCY = frequencyFromVanilla(200f, 16) / XZSCALE;
    public static final float VANILLA_SELECTOR_NOISE_FREQUENCY_XZ = frequencyFromVanilla(684.412f / 80.0f, 8) / XZSCALE;
    public static final float VANILLA_SELECTOR_NOISE_FREQUENCY_Y = frequencyFromVanilla(684.412f / 80.0f, 8) / YSCALE;

    public static final float VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ = frequencyFromVanilla(684.412f, 16) / XZSCALE;
    public static final float VANILLA_LOWHIGH_NOISE_FREQUENCY_Y = frequencyFromVanilla(684.412f, 16) / YSCALE;

    public static float biomeHeightVariationVanilla(float heightVariation) {
        return 2.4f * heightVariation + 4.0f / 15.0f;
    }

    public static float biomeHeightVanilla(float height) {
        return height * 17.0f / 64.0f - 1.0f / 256.0f;
    }

    // vanilla adds noise of frequencies: f, f*2, f*4, f*8 etc...
    // for 1 octave: 1*f = f*2^0 = f*2^(octaves-1)
    // for 2 octaves: 2*f = f*2^1 = f*2^(octaves-1)
    // same for 3 and higher
    public static float frequencyFromVanilla(float freq, int octaves) {
        return (freq / (1 << (octaves - 1)));
    }


    public static float maxValueMultipler(int octaves) {
        return (1 << octaves) - 1;
    }

    public static void initFlowNoiseHack() {
        Random random = new Random(123456789);
        for (int i = 0; i < Utils.RANDOM_VECTORS.length / 4; i++) {
            int j = random.nextInt(NoiseGeneratorImproved.GRAD_X.length);
            Utils.RANDOM_VECTORS[i*4] = NoiseGeneratorImproved.GRAD_X[j];
            Utils.RANDOM_VECTORS[i*4 + 1] = NoiseGeneratorImproved.GRAD_Y[j];
            Utils.RANDOM_VECTORS[i*4 + 1] = NoiseGeneratorImproved.GRAD_Z[j];
        }
    }
}
