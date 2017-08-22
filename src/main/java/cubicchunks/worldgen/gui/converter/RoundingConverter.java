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
package cubicchunks.worldgen.gui.converter;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;

import com.google.common.base.Converter;
import net.minecraft.util.math.MathHelper;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;

public class RoundingConverter extends Converter<Float, Float> {

    private final BiPredicate<Double, Double> isValueInRadius;
    private final Set<RoundingEntry> roundingData;
    private final float maxExp;
    private Converter<Float, Float> reverse;

    RoundingConverter(Converters.RoundingBuilder builder) {
        this.isValueInRadius = builder.isValueInRadius;
        this.roundingData = builder.roundingData;
        this.maxExp = builder.maxExp;
    }

    void setReverse(Converter<Float, Float> reverse) {
        this.reverse = reverse;
    }

    @Override protected Float doForward(Float input) {
        double max = -Double.MAX_VALUE;
        double best = 0;

        final double slideValue = reverse.convert(input);

        int startExponent = MathHelper.ceil(maxExp);
        for (RoundingEntry e : roundingData) {
            for (int trySnapDivExp = startExponent; ; trySnapDivExp--) {
                double roundValue = getRoundValue(input, trySnapDivExp, e);
                if (Double.isNaN(roundValue) || Double.isInfinite(roundValue)) {
                    break;
                }
                double roundedSlideValue = reverse.convert((float) roundValue);
                if (isValueInRadius.test(slideValue, roundedSlideValue)) {
                    double v = getDivisor(trySnapDivExp, e);
                    if (v > max) {
                        max = v;
                        best = roundValue;
                    }
                    break;
                }
            }
        }
        return (float) best;
    }

    private double getDivisor(int trySnapDivExp, RoundingEntry e) {
        return pow(e.baseVal, trySnapDivExp) * e.multiplier;
    }

    private double getRoundValue(double rawValue, int exp, RoundingEntry e) {
        double divisor = getDivisor(exp, e);
        if (divisor == 0) {
            return Double.NaN;
        }
        return round(rawValue / divisor) * divisor;
    }

    @Override protected Float doBackward(Float value) {
        return value;
    }

    static final class RoundingEntry {

        private final double baseVal, multiplier;

        RoundingEntry(double baseVal, double multiplier) {
            this.baseVal = baseVal;
            this.multiplier = multiplier;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RoundingEntry that = (RoundingEntry) o;

            if (Double.compare(that.baseVal, baseVal) != 0) {
                return false;
            }
            return Double.compare(that.multiplier, multiplier) == 0;

        }

        @Override public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(baseVal);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(multiplier);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
