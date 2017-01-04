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

    public static final float VANILLA_DEPTH_NOISE_FREQUENCY = 200f / (2 << 15) / XZSCALE;
    public static final float VANILLA_SELECTOR_NOISE_FREQUENCY_XZ = (684.412f / 80.0f) / (2 << 7) / XZSCALE;
    public static final float VANILLA_SELECTOR_NOISE_FREQUENCY_Y = (684.412f / 160.0f) / (2 << 7) / YSCALE;

    public static final float VANILLA_LOWHIGH_NOISE_FREQUENCY_XZ = (684.412f) / (2 << 15) / XZSCALE;
    public static final float VANILLA_LOWHIGH_NOISE_FREQUENCY_Y = (684.412f) / (2 << 15) / YSCALE;

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
        return (float) (freq / Math.pow(2, octaves - 1));
    }
}
