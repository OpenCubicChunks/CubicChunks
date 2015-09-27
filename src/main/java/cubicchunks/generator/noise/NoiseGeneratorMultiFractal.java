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

public class NoiseGeneratorMultiFractal {

	private NoiseGeneratorSimplex[] generators;
	private int numOctaves;

	public NoiseGeneratorMultiFractal(final Random rand, final int octaves) {
		this.numOctaves = octaves;
		this.generators = new NoiseGeneratorSimplex[octaves];

		for (int i = 0; i < octaves; ++i) {
			this.generators[i] = new NoiseGeneratorSimplex(rand);
		}
	}

	public double[] getNoiseMap(final double[] noiseMap, final double xOffset, final double zOffset, final int xSize,
			final int zSize, final double xScale, final double zScale, final double lacunarity) {
		return calculate(noiseMap, xOffset, zOffset, xSize, zSize, xScale, zScale, lacunarity, 0.5D);
	}

	public double[] calculate(double[] noiseArray, final double xOffset, final double zOffset, final int xSize,
			final int zSize, final double xScale, final double zScale, final double lacunarity, final double gain) {
		if (noiseArray != null && noiseArray.length >= xSize * zSize) {
			for (int i = 0; i < noiseArray.length; ++i) // clear the array
			{
				noiseArray[i] = 0.0D;
			}
		} else {
			noiseArray = new double[xSize * zSize];
		}

		double amplitude = 1.0D;
		double frequency = 1.0D;

		for (int octave = 0; octave < this.numOctaves; ++octave) {
			this.generators[octave].getValueArray(noiseArray, xOffset, zOffset, xSize, zSize, xScale * frequency
					* amplitude, zScale * frequency * amplitude, 0.55D / amplitude);
			frequency *= lacunarity;
			amplitude *= gain;
		}

		return noiseArray;
	}
}