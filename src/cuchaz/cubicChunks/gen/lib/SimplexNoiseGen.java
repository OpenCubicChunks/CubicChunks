/*
 * Copyright (C) 2014 Nick Whitney 
 */

package cuchaz.cubicChunks.gen.lib;

public class SimplexNoiseGen
{
	static final int X_NOISE_GEN = 1619;
	static final int Y_NOISE_GEN = 31337;
	static final int Z_NOISE_GEN = 6971;
	static final int SEED_NOISE_GEN = 1013;
	static final int SHIFT_NOISE_GEN = 8;

	private static int fastfloor(double x)
	{
		return (x > 0) ? (int) x : (int) x - 1;
	}

	public static double SimplexNoise3D (double x, double y, double z, int seed, NoiseQuality noiseQuality)
	{
		// Skew the input space to determine which simplex cell we're in
		final double F3 = 1.0/3.0;

		double s = (x + y + z) * F3; // nice and simple

		int i = fastfloor(x + s);
		int j = fastfloor(y + s);
		int k = fastfloor(z + s);	

		final double G3 = 1.0/6.0; // nice and simple unskew factor

		double t = (i + j + k) * G3;

		double X0 = i - t;
		double Y0 = j - t;
		double Z0 = k - t;

		// Map the difference between the coordinates of the input value and the
		// coordinates of the cube's outer-lower-left vertex onto an S-curve.
		double x0 = 0, y0 = 0, z0 = 0;

		switch (noiseQuality)
		{
		case QUALITY_FAST:
			x0 = (x - X0);
			y0 = (y - Y0);
			z0 = (z - Z0);
			break;
		case QUALITY_STD:
			x0 = Interp.SCurve3 (x - X0);
			y0 = Interp.SCurve3 (y - Y0);
			z0 = Interp.SCurve3 (z - Z0);
			break;
		case QUALITY_BEST:
			x0 = Interp.SCurve5 (x - X0);
			y0 = Interp.SCurve5 (y - Y0);
			z0 = Interp.SCurve5 (z - Z0);
			break;
		}

		// For the 3D case, the simplex shape is a slightly irregular tetrahedron
		// Determine which simplex we are in	
		int i1, j1, k1;
		int i2, j2, k2;

		if (x0 >= y0)
		{
			if (y0 >= z0) 		// XYZ order
			{
				i1 = 1;
				j1 = 0;
				k1 = 0;
				i2 = 1;
				j2 = 1;
				k2 = 0;
			}
			else if (x0 >= z0) 	// XZY order
			{
				i1 = 1;
				j1 = 0;
				k1 = 0;
				i2 = 1;
				j2 = 0;
				k2 = 1;
			}
			else 				// ZXY order
			{
				i1 = 0;
				j1 = 0;
				k1 = 1;
				i2 = 1;
				j2 = 0;
				k2 = 1;
			}
		}
		else
		{
			if (y0 < z0)		 // ZYX order
			{
				i1 = 0;
				j1 = 0;
				k1 = 1;
				i2 = 0;
				j2 = 1;
				k2 = 1;
			}
			else if (x0 < z0) 	// YZX order
			{
				i1 = 0;
				j1 = 1;
				k1 = 0;
				i2 = 0;
				j2 = 1;
				k2 = 1;
			}
			else				// YXZ order
			{
				i1 = 0;
				j1 = 1;
				k1 = 0;
				i2 = 1;
				j2 = 1;
				k2 = 0;
			}
		}

		// A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
		// a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
		// a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
		// c = 1/6

		double x1 = x0 - i1 + G3;		// Offsets for second corner in (x,y,z) coords
		double y1 = y0 - j1 + G3;
		double z1 = z0 - k1 + G3;
		double x2 = x0 - i2 + 2.0*G3;	// Offsets for third corner in (x,y,z) coords
		double y2 = y0 - j2 + 2.0*G3;
		double z2 = z0 - k2 + 2.0*G3;
		double x3 = x0 - 1.0 + 3.0*G3;	// Offsets for last corner in (x,y,z) coords
		double y3 = y0 - 1.0 + 3.0*G3;
		double z3 = z0 - 1.0 + 3.0*G3;

		//		// Now calculate the noise values at each vertex of the cube.  To generate
		//		// the coherent-noise value at the input point, interpolate these eight
		//		// noise values using the S-curve value as the interpolant (trilinear
		//		// interpolation.)
		//		
		//		double n000  = GradientNoise3D (x, y, z, i, j, k, seed);
		//		double n100  = GradientNoise3D (x, y, z, x1, j, k, seed);
		//		double n010  = GradientNoise3D (x, y, z, i, y1, k, seed);
		//		double n110  = GradientNoise3D (x, y, z, x1, y1, k, seed);
		//		double n001  = GradientNoise3D (x, y, z, i, j, z1, seed);
		//		double n101  = GradientNoise3D (x, y, z, x1, j, z1, seed);
		//		double n011  = GradientNoise3D (x, y, z, i, y1, z1, seed);
		//		double n111  = GradientNoise3D (x, y, z, x1, y1, z1, seed);
		//		
		//		double nx00  = Interp.linearInterp (n000, n100, x0);
		//		double nx01  = Interp.linearInterp (n001, n101, x0);
		//		double nx10  = Interp.linearInterp (n010, n110, x0);
		//		double nx11  = Interp.linearInterp (n011, n111, x0);
		//		
		//		double nxy0  = Interp.linearInterp (nx00, nx10, y0);
		//		double nxy1  = Interp.linearInterp (nx01, nx11, y0);
		//
		//		return Interp.linearInterp (nxy0, nxy1, z0);

		double n0, n1, n2, n3; // Noise contributions from the four corners

		// Calculate the contribution from the four corners
		double t0 = 0.5 - x0*x0 - y0*y0 - z0*z0;

		if (t0 < 0)	n0 = 0.0;
		else
		{
			t0 *= t0;
			n0 = t0 * t0 * GradientNoise3D(x, y, z, x0, y0, z0, seed);
		}

		double t1 = 0.5 - x1*x1 - y1*y1 - z1*z1;

		if (t1 < 0) n1 = 0.0;
		else
		{
			t1 *= t1;
			n1 = t1 * t1 * GradientNoise3D(x, y, z, x1, y1, z1, seed);
		}

		double t2 = 0.5 - x2*x2 - y2*y2 - z2*z2;

		if (t2 < 0) n2 = 0.0;
		else
		{
			t2 *= t2;
			n2 = t2 * t2 * GradientNoise3D(x, y, z, x2, y2, z2, seed);
		}

		double t3 = 0.5 - x3*x3 - y3*y3 - z3*z3;

		if (t3 < 0) n3 = 0.0;
		else
		{
			t3 *= t3;
			n3 = t3 * t3 * GradientNoise3D(x, y, z, x3, y3, z3, seed);
		}

		return 32.0 * (n0 + n1 + n2 + n3);
	}

	public static double GradientNoise3D (double fx, double fy, double fz, 
			double ix,	double iy, double iz, 
			int seed)
	{
		VectorTable vectorTable = new VectorTable();

		// Randomly generate a gradient vector given the integer coordinates of the
		// input value.  This implementation generates a random number and uses it
		// as an index into a normalized-vector lookup table.
		int vectorIndex = (X_NOISE_GEN * (int)ix
				+ Y_NOISE_GEN * (int)iy
				+ Z_NOISE_GEN * (int)iz
				+ SEED_NOISE_GEN * seed)
				& 0xffffffff;

		vectorIndex ^= (vectorIndex >> SHIFT_NOISE_GEN);
		vectorIndex &= 0xff;

		double xvGradient = vectorTable.getRandomVectors(vectorIndex, 0);
		double yvGradient = vectorTable.getRandomVectors(vectorIndex, 1);
		double zvGradient = vectorTable.getRandomVectors(vectorIndex, 2);
		// array size too large when using this original, changed to above for all 3
		// double zvGradient = vectorTable.getRandomVectors(vectorIndex << 2, 2);

		// Set up us another vector equal to the distance between the two vectors
		// passed to this function.
		double xvPoint = (fx - ix);
		double yvPoint = (fy - iy);
		double zvPoint = (fz - iz);

		// Now compute the dot product of the gradient vector with the distance
		// vector.  The resulting value is gradient noise.  Apply a scaling value
		// so that this noise value ranges from -1.0 to 1.0.
		return ((xvGradient * xvPoint)
				+ (yvGradient * yvPoint)
				+ (zvGradient * zvPoint)) * 2.12;
	}

	/** Modifies a floating-point value so that it can be stored in a
	 * int32 variable.
	 *
	 * @param n A floating-point number.
	 *
	 * @returns The modified floating-point number.
	 *
	 * This function does not modify @a n.
	 *
	 * In libnoise, the noise-generating algorithms are all integer-based;
	 * they use variables of type int32.  Before calling a noise
	 * function, pass the @a x, @a y, and @a z coordinates to this function to
	 * ensure that these coordinates can be cast to a int32 value.
	 *
	 * Although you could do a straight cast from double to int32, the
	 * resulting value may differ between platforms.  By using this function,
	 * you ensure that the resulting value is identical between platforms.
	 */
	public static double MakeInt32Range (double n)
	{
		if (n >= 1073741824.0)
			return (2.0 * (n % 1073741824.0)) - 1073741824.0;
		else if (n <= -1073741824.0)
			return (2.0 * (n % 1073741824.0)) + 1073741824.0;
		else
			return n;
	}

}
