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
package cubicchunks.util;

import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MathUtil {

    public static double lerp(final double a, final double min, final double max) {
        return min + a * (max - min);
    }

    // reverse linear interpolation - unlerp(lerp(a, min, max), min, max) == a
    public static double unlerp(final double v, final double min, final double max) {
        return (v - min) / (max - min);
    }

    public static float unlerp(final float v, final float min, final float max) {
        return (v - min) / (max - min);
    }

    public static float lerp(final float a, final float min, final float max) {
        return min + a * (max - min);
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int min(int a, int b, int c, int d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    public static int min(int... a) {
        int min = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }
        return min;
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static int max(int a, int b, int c) {
        return Math.max(Math.max(a, b), c);
    }

    public static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public static int max(int... a) {
        int max = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }
        return max;
    }

    public static float maxIgnoreNan(float... a) {
        float max = a[0];
        for (int i = 1; i < a.length; i++) {
            if (max != max || a[i] > max) {
                max = a[i];
            }
        }
        if (max != max) {
            throw new IllegalArgumentException("All values are NaN");
        }
        return max;
    }

    public static double gaussianProbabilityDensity(double x, double mean, double stdDev) {
        return exp(-(x - mean) * (x - mean) / (2 * stdDev * stdDev)) /
                (sqrt(2 * Math.PI) * stdDev);
    }
	
    public static double gaussianProbabilityCyclic(int x, int mean, double stdDev, int spacing) {
        /* Modulo works from 0 to limit-1.
           I want it to go from -limit/2 to +limit/2 because the curve is centered on 0 and thus doesn't start there.
           By moving all values by half of the spacing to the right (the "- halfspace" part in "factor"),
           I effectively move the middle of the curve from 0 to the middle of the mod range.
           Due to moving the curve by spacing/2 for the mod (to not cut curve parts), the values are off by half the spacing,
           so I have to move them back to their original position (the " - halfspace" in "shiftedLoc").
           In theory that could also be "+ halfspace" but since the curve is periodic it doesn't matter.
           The "- mean" in "shiftedLoc" moves the center of the 1st curve to the right, so into + range.
         */
        //Using vars for better overview.
        double halfSpace = (double)spacing / 2.0;
        double shiftedLoc = Math.abs((double)x - halfSpace - (double)mean);
        double factor = (shiftedLoc % (double)spacing) - halfSpace;
        double divide = 2.0 * stdDev * stdDev;
        double exponent = (-1.0 * factor) * factor / divide;
        double result = exp(exponent);
        return result;
    }

    public static boolean rangesIntersect(int min1, int max1, int min2, int max2) {
        return min1 <= max2 && min2 <= max1;
    }
}
