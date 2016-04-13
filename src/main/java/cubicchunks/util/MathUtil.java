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

import java.util.Random;

public class MathUtil {

	public static double lerp(final double a, final double min, final double max) {
		return min + a * (max - min);
	}

	public static int randRange(Random rand, int start, int end) {
		return rand.nextInt(end - start + 1) + start;
	}

	public static int minInteger(Integer a, Integer b) {
		if (a == null || b == null) {
			return Integer.MIN_VALUE;
		}
		return Math.min(a, b);
	}

	public static int maxInteger(Integer a, Integer b) {
		if (a == null && b == null) {
			return Integer.MIN_VALUE;
		}
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return Math.max(a, b);
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
			if (a[i] < min) min = a[i];
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
			if (a[i] > max) max = a[i];
		}
		return max;
	}
}
