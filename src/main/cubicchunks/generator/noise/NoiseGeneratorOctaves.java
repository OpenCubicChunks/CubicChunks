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

import net.minecraft.util.MathHelper;

public class NoiseGeneratorOctaves extends NoiseGenerator {
	
	/**
	 * Collection of noise generation functions. Output is combined to produce different octaves of noise.
	 */
	private NoiseGeneratorImproved[] generatorCollection;
	private int octaves;
	
	public NoiseGeneratorOctaves(Random rand, int numOctaves) {
		this.octaves = numOctaves;
		this.generatorCollection = new NoiseGeneratorImproved[numOctaves];
		
		for (int var3 = 0; var3 < numOctaves; ++var3) {
			this.generatorCollection[var3] = new NoiseGeneratorImproved(rand);
		}
	}
	
	public double[] generateNoiseOctaves(double[] noiseArray, int noiseX, int noiseY, int noiseZ, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		if (noiseArray == null) // if noise array doesn't exist, create it
		{
			noiseArray = new double[xSize * ySize * zSize];
		} else // clear the array if it already exists
		{
			for (int i = 0; i < noiseArray.length; ++i) {
				noiseArray[i] = 0.0D;
			}
		}
		
		double frequency = 1.0D;
		
		for (int curOctave = 0; curOctave < this.octaves; ++curOctave) {
			double xValue = noiseX * frequency * xScale;
			double yValue = noiseY * frequency * yScale;
			double zValue = noiseZ * frequency * zScale;
			
			long xLong = MathHelper.floor(xValue); // convert the double to a long int and floor it
			long zLong = MathHelper.floor(zValue);
			
			xValue -= xLong; // subtract the double-cast long from the double??? This should get the decimal portion of xValue
			zValue -= zLong;
			xLong %= 16777216L; // binary select the long
			zLong %= 16777216L;
			xValue += xLong; // add the double-cast long after the binary select to the double
			zValue += zLong;
			
			this.generatorCollection[curOctave].populateNoiseArray(noiseArray, xValue, yValue, zValue, xSize, ySize, zSize, xScale * frequency, yScale * frequency, zScale * frequency, frequency);
			frequency /= 2.0D;
		}
		
		return noiseArray;
	}
	
	/**
	 * Bouncer function to the main one with some default arguments.
	 */
	public double[] generateNoiseOctaves(double[] par1ArrayOfDouble, int par2, int par3, int par4, int par5, double par6, double par8, double par10) {
		return this.generateNoiseOctaves(par1ArrayOfDouble, par2, 10, par3, par4, 1, par5, par6, 1.0D, par8);
	}
}
