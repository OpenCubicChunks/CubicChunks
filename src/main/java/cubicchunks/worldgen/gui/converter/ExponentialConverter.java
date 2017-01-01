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

import com.google.common.base.Converter;

import javax.annotation.ParametersAreNonnullByDefault;

import mcp.MethodsReturnNonnullByDefault;

import static cubicchunks.util.MathUtil.lerp;
import static cubicchunks.util.MathUtil.unlerp;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static net.minecraft.util.math.MathHelper.ceil;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ExponentialConverter extends Converter<Float, Float> {

	private final boolean hasZero;
	private final float minExpPos;
	private final float maxExpPos;
	private final float minExpNeg;
	private final float maxExpNeg;
	private final float baseValue;
	private final double zeroPos;
	private final double positiveExpStart;
	private final double negativeExpStart;
	private final double minLinearPosVal;
	private final double minLinearNegVal;

	ExponentialConverter(Converters.ExponentialBuilder builder) {
		boolean hasPositivePart = !Float.isNaN(builder.minExpPos) && !Double.isNaN(builder.maxExpPos);
		boolean hasNegativePart = !Float.isNaN(builder.minExpNeg) && !Double.isNaN(builder.maxExpNeg);
		if (!hasPositivePart) {
			builder.minExpPos = 0;
			builder.maxExpPos = 0;
		}
		if (!hasNegativePart) {
			builder.minExpNeg = 0;
			builder.maxExpNeg = 0;
		}
		this.hasZero = builder.hasZero;
		this.minExpNeg = builder.minExpNeg;
		this.maxExpNeg = builder.maxExpNeg;
		this.minExpPos = builder.minExpPos;
		this.maxExpPos = builder.maxExpPos;
		this.baseValue = builder.baseVal;

		// find zeroX, minPosX, maxNegX
		// to solve:
		//
		// d/dposMinX(linearPartPositive(x, negMaxX, posMinX, zeroX)) = d/dposMinX(exponentialPartPositive(x, negMaxX, posMinX)) for x=posMinX
		// d/dnegMaxX(linearPartNegative(x, negMaxX, posMinX, zeroX)) = d/dnegMaxX(exponentialPartNegative(x, negMaxX, posMinX)) for x=negMaxX
		// (1-posMinX)/(maxPositiveExponent-minPositiveExponent) = negMaxX/(maxNegativeExponent-minNegativeExponent)
		//            ^ here I did a mistake, it's supposed to be division. I'm ot redoing it all from the scratch, I will just do result = 1 - result
		//
		// solve for: posMinX, negMaxX, zeroX
		//
		//      linearPartPositive(x, negMaxX, posMinX, zeroX) =
		//          = [(x - zeroX)/(posMinX - zeroX)]*(baseValue^minExpPos)
		//      linearPartNegative(x, negMaxX, posMinX, zeroX) =
		//          = [(x - negMaxX)/(zeroX - negMaxX)]*(baseValue^minExpNeg) - baseValue^minExpNeg
		//
		//      exponentialPartPositive(x, negMaxX, posMinX) =
		//          = baseValue^[(x - posMinX)/(1 - posMinX)*(maxExpPos-minExpPos)+minExpPos]
		//      exponentialPartNegative(x, negMaxX, posMinX) =
		//          = -1*baseValue^[[1 - (x - negMinX)/(negMaxX - negMinX)]*(maxExpNeg-minExpNeg)+minExpNeg]
		//
		// Now the ugly part: I don't want to solve it by hand,
		// so I need to turn it into format that I can put into wolfram alpha:
		//
		// x        -> x
		// zeroX    -> z
		// posMinX  -> p
		// negMaxX  -> n
		// baseValue-> b
		// minExpPos-> c
		// maxExpPos-> d
		// minExpNeg-> f //no e, because e is constant
		// maxExpNeg-> g
		//
		//      d/dx(((x - z)/(p - z))*(b^c)) = d/dx(b^((x - p)/(1 - p)*(d-c)+c))                   for x == p
		//      d/dx(((x - n)/(z - n))*(b^f) - b^f) = d/dx(-1*b^((1 - x/n)*(g-f)+f))                for x == n
		//      (1-p)/(d-c) = n/(g-f)
		//
		// now if we put it into wolfram alpha... we find out that wolfram alpha refuses to cooperate.
		// so let's try in smaller pieces. First get rid of derivatives and turn it into wolfram language:
		//
		//      b^c/(p - z) == (b^(c + ((-c + d) (-p + x))/(1 - p)) (-c + d) Log[b])/(1 - p)        for x == p
		//      b^f/(-n + z) == (b^(f + (-f + g) (1 - x/n)) (-f + g) Log[b])/n                      for x == n
		//      (1 - p)/(d - c) == n/(g - f)
		//
		//      b^c/(p - z) == (b^(c + ((-c + d) (-p + p))/(1 - p)) (-c + d) Log[b])/(1 - p) <==> b^c/(p - z) == (b^c (-c + d) Log[b])/(1 - p)
		//      b^f/(-n + z) == (b^(f + (-f + g) (1 - n/n)) (-f + g) Log[b])/n <==> b^f/(-n + z) == (b^f (-f + g) Log[b])/n
		//      (1 - p)/(d - c) == n/(g - f)
		//
		//      b^c/(p - z) == (b^c (-c + d) Log[b])/(1 - p) -- substitute p from below and solve for z -->     z == (-1 + f Log[b] - g Log[b])/(-2 + c Log[b] - d Log[b] + f Log[b] - g Log[b])
		//      b^f/(-n + z) == (b^f (-f + g) Log[b])/n     -- substitute n from below and solve for p -->      p == ((-1 + (f - g - c z + d z) Log[b])/(-1 + (f - g) Log[b]))
		//      (1 - p)/(d - c) == n/(g - f)  -- solve for n -->                                                n == (-(((f - g) (-1 + p))/(c - d)))
		//
		// So the solution for z is:
		//
		//      z == (-1 + f Log[b] - g Log[b])/(-2 + c Log[b] - d Log[b] + f Log[b] - g Log[b])
		//
		// Now that we have z we can express p and n in terms of it:
		//
		//      p == (-1 + c z Log[b] - d z Log[b])/(-1 + c Log[b] - d Log[b])
		//      n == ((f - g)*z*Log[b])/(-1 + f*Log[b] - g*Log[b])

		double b = baseValue;
		double lb = log(b);
		double c = minExpPos;
		double d = maxExpPos;
		double f = minExpNeg;
		double g = maxExpNeg;
		double z = (-1 + f*lb - g*lb)/(-2 + c*lb - d*lb + f*lb - g*lb);
		if (!hasNegativePart && hasPositivePart) {
			z = 0;
		} else if (hasNegativePart && !hasPositivePart) {
			z = 1;
		} else if (!hasNegativePart || !hasPositivePart) {
			throw new IllegalArgumentException("Converter must have at least either positive or negative part");
		}
		double p = (-1 + c*z*lb - d*z*lb)/(-1 + c*lb - d*lb);
		double n = ((f - g)*z*lb)/(-1 + f*lb - g*lb);

		this.zeroPos = z;
		this.positiveExpStart = hasZero ? p : z;
		this.negativeExpStart = hasZero ? n : z;

		this.minLinearPosVal = hasPositivePart ? pow(baseValue, minExpPos) : Double.POSITIVE_INFINITY;
		this.minLinearNegVal = hasNegativePart ? -pow(baseValue, minExpNeg) : Double.NEGATIVE_INFINITY;
	}

	@Override
	protected Float doForward(Float x) {
		return (float) doForwardsDouble(x);
	}

	private double doForwardsDouble(double x) {
		// for x below negative start - negative exponential
		// for x between negMaxX and posMinX - linear
		// for x above posMinX - positive exponential
		double negMinX = 0;
		double negMaxX = getNegStartX();
		double zeroX = getZeroX();
		double posMinX = getPosStartX();
		double posMaxX = 1;

		if (x < negMaxX) {
			// scale x to be between 0 and 1 (undo linear interpolation) and reverse order for negatives, so that values closer to 1 are further away from 0
			x = 1 - unlerp(x, negMinX, negMaxX);
			// interpolate for exponents
			x = lerp(x, minExpNeg, maxExpNeg);
			return -pow(baseValue, x);
		}
		if (x > posMinX) {
			// scale x to be between 0 and 1 (undo linear interpolation)
			x = unlerp(x, posMinX, posMaxX);
			x = lerp(x, minExpPos, maxExpPos);
			return pow(baseValue, x);
		}
		// handle the linear part

		if (x < zeroX) {
			// negative linear part
			double minNegXLin = negMaxX;
			double maxNegXLin = zeroX;
			x = unlerp(x, minNegXLin, maxNegXLin);
			return lerp(x, minLinearNegVal, 0);
		} else {
			// positive linear part
			double minPosXLin = zeroX;
			double maxPosXLin = posMinX;
			x = unlerp(x, minPosXLin, maxPosXLin);
			return lerp(x, 0, minLinearPosVal);
		}
	}

	private double getPosStartX() {
		return positiveExpStart;
	}

	private double getZeroX() {
		return zeroPos;
	}

	private double getNegStartX() {
		return negativeExpStart;
	}


	@Override protected Float doBackward(Float x) {
		return (float) doBackwardDouble(x);
	}

	private double doBackwardDouble(double value) {
		// linear part for positive
		if (value >= 0 && value <= minLinearPosVal) {
			/*
			 * Code to reverse:
			 * double minPosXLin = zeroX;
			 * double maxPosXLin = posMinX;
			 * x = unlerp(x, minPosXLin, maxPosXLin);
			 * return lerp(x, 0, minLinearPosVal);
			 */
			double maxPosXLin = getPosStartX();
			double minPosXLin = getZeroX();
			double x = unlerp(value, 0, minLinearPosVal);
			x = lerp(x, minPosXLin, maxPosXLin);
			return x;
		}
		// linear part for negative
		if (value <= 0 && value >= minLinearNegVal) {
			/*
			 * Code to reverse:
			 * double minNegXLin = negMaxX;
			 * double maxNegXLin = zeroX;
			 * x = unlerp(x, minNegXLin, maxNegXLin);
			 * return lerp(x, minLinearNegVal, 0);
			 */
			double minNegXLin = getNegStartX();
			double maxNegXLin = getZeroX();
			double x = unlerp(value, minLinearNegVal, 0);
			x = lerp(x, minNegXLin, maxNegXLin);
			return x;
		}
		double negMinX = 0;
		double negMaxX = getNegStartX();
		double posMinX = getPosStartX();
		double posMaxX = 1;
		if (value >= minLinearPosVal) {
			/*
			 * Code to reverse:
			 * x = unlerp(x, posMinX, posMaxX);
			 * x = lerp(x, minExpPos, maxExpPos);
			 * return pow(baseValue, x);
			 */
			double x = log(value)/log(baseValue);
			x = unlerp(x, minExpPos, maxExpPos);
			x = lerp(x, posMinX, posMaxX);
			return x;
		} else {
			assert value <= minLinearNegVal;
			/*
			 * Code to reverse:
			 * // x = 1 - unlerp(x, negMinX, negMaxX); --> equivalent
			 * x = unlerp(x, negMinX, negMaxX);
			 * x = 1 - x;
			 * x = lerp(x, minExpNeg, maxExpNeg);
			 * return -pow(baseValue, x);
			 */
			double x = log(-value)/log(baseValue);
			x = unlerp(x, minExpNeg, maxExpNeg);
			x = 1 - x;
			x = lerp(x, negMinX, negMaxX);
			return x;
		}
	}
}
