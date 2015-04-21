/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.generator.noise;

import java.util.Random;

public class NoiseGeneratorSimplex {

	private static Grad[] GRAD3 = { new Grad(1, 1, 0), new Grad(-1, 1, 0), new Grad(1, -1, 0), new Grad(-1, -1, 0),
		new Grad(1, 0, 1), new Grad(-1, 0, 1), new Grad(1, 0, -1), new Grad(-1, 0, -1), new Grad(0, 1, 1),
		new Grad(0, -1, 1), new Grad(0, 1, -1), new Grad(0, -1, -1) };
	public static final double SQRT3 = Math.sqrt(3.0D);
	private static final double F2 = 0.5D * (SQRT3 - 1.0D);
	private static final double G2 = (3.0D - SQRT3) / 6.0D;

	// Inner class to speed up gradient computations
	// (array access is a lot slower than member access)
	private static class Grad {
		double x, y, z;

		Grad(final double x, final double y, final double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private int[] perm;
	public double xCoord;
	public double zCoord;

	public NoiseGeneratorSimplex() {
		this(new Random());
	}

	public NoiseGeneratorSimplex(final Random rand) {
		this.perm = new int[512];
		this.xCoord = rand.nextDouble() * 256.0D;
		this.zCoord = rand.nextDouble() * 256.0D;
		int i;

		for (i = 0; i < 256; this.perm[i] = i++) {
			;
		}

		for (i = 0; i < 256; ++i) {
			int temp = rand.nextInt(256 - i) + i;
			this.perm[i] = this.perm[temp];
			this.perm[temp] = this.perm[i];
			this.perm[i + 256] = this.perm[i];
		}
	}

	private static int fastfloor(final double x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	private static double dot(final Grad g, final double x, final double y) {
		return g.x * x + g.y * y;
	}

	public void getValueArray(final double[] noiseArray, final double xOffset, final double zOffset, final int xSize,
			final int zSize, final double xScale, final double zScale, final double scale) {
		int counter = 0;

		for (int z = 0; z < zSize; ++z) {
			double yIn = (zOffset + z) * zScale + this.zCoord;

			for (int x = 0; x < xSize; ++x) {
				double xIn = (xOffset + x) * xScale + this.xCoord;

				noiseArray[counter++] += getValue(xIn, yIn) * scale;
			}
		}
	}

	public double getValue(final double xIn, final double yIn) {
		// Skew the input space to determine the simplex cell we're in
		double s = (xIn + yIn) * F2;

		int i = fastfloor(xIn + s);
		int j = fastfloor(yIn + s);

		double t = (i + j) * G2;

		// Unskew the cell origin back to (x,y) space
		double X0 = i - t;
		double Y0 = j - t;

		// the x,y distances from the cell origin
		double x0 = xIn - X0;
		double y0 = yIn - Y0;

		// For the 2D case, the simplex shape is an equilateral triangle.
		// Determine which simplex we are in.

		// Offsets for second (middle) corner of simplex in (i,j) coords
		byte i1;
		byte j1;

		if (x0 > y0) { // lower triangle, XY order: (0,0)->(1,0)->(1,1)
			i1 = 1;
			j1 = 0;
		} else { // upper triangle, YX order: (0,0)->(0,1)->(1,1)
			i1 = 0;
			j1 = 1;
		}

		// A step of (1, 0) in (i, j) means a step of (1 - c, -c) in (x, y), and
		// a step of (0, 1) in (i, j) means a step of (-c, 1 - c) in (x, y), where
		// c = (3 - sqrt(3)) / 6

		// Offsets for middle corner in (x,y) unskewed coords
		double x1 = x0 - i1 + G2;
		double y1 = y0 - j1 + G2;

		// Offsets for last corner in (x,y) unskewed coords
		double x2 = x0 - 1.0D + 2.0D * G2;
		double y2 = y0 - 1.0D + 2.0D * G2;

		// Work out the hashed gradient indices of the three simplex corners
		int ii = i & 255;
		int jj = j & 255;

		int gi0 = this.perm[ii + this.perm[jj]] % 12;
		int gi1 = this.perm[ii + i1 + this.perm[jj + j1]] % 12;
		int gi2 = this.perm[ii + 1 + this.perm[jj + 1]] % 12;

		// Calculate the contribution from the three corners
		double t0 = 0.5D - x0 * x0 - y0 * y0;
		double n0;

		if (t0 < 0.0D) {
			n0 = 0.0D;
		} else {
			t0 *= t0;
			n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0);
		}

		double t1 = 0.5D - x1 * x1 - y1 * y1;
		double n1;

		if (t1 < 0.0D) {
			n1 = 0.0D;
		} else {
			t1 *= t1;
			n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1);
		}

		double t2 = 0.5D - x2 * x2 - y2 * y2;
		double n2;

		if (t2 < 0.0D) {
			n2 = 0.0D;
		} else {
			t2 *= t2;
			n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2);
		}

		return 70.0D * (n0 + n1 + n2);
	}
}
