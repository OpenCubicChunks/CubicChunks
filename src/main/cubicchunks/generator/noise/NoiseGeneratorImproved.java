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

public class NoiseGeneratorImproved extends NoiseGenerator {
	
	private int[] permutations;
	public double xCoord;
	public double yCoord;
	public double zCoord;
	
	public NoiseGeneratorImproved() {
		this(new Random());
	}
	
	public NoiseGeneratorImproved(Random rand) {
		this.permutations = new int[512];
		
		// this looks like a turbulence modifier
		this.xCoord = rand.nextDouble() * 256.0D;
		this.yCoord = rand.nextDouble() * 256.0D;
		this.zCoord = rand.nextDouble() * 256.0D;
		
		int i;
		
		for (i = 0; i < 256; this.permutations[i] = i++) // fill the permutation array
		{
			;
		}
		
		for (i = 0; i < 256; ++i) // randomize the permutation array 256 times
		{
			int moveTo = rand.nextInt(256 - i) + i;
			int moveFrom = this.permutations[i];
			this.permutations[i] = this.permutations[moveTo];
			this.permutations[moveTo] = moveFrom;
			this.permutations[i + 256] = this.permutations[i]; // double the array length
		}
	}
	
	public final double lerp(double alpha, double min, double max) {
		return min + alpha * (max - min);
	}
	
	public final double func_76309_a(int i, double a, double b) {
		int var6 = i & 15;
		double var7 = (1 - ( (var6 & 8) >> 3)) * a;
		double var9 = var6 < 4 ? 0.0D : (var6 != 12 && var6 != 14 ? b : a);
		return ( (var6 & 1) == 0 ? var7 : -var7) + ( (var6 & 2) == 0 ? var9 : -var9);
	}
	
	public final double grad(int par1, double par2, double par4, double par6) {
		int var8 = par1 & 15;
		double var9 = var8 < 8 ? par2 : par4;
		double var11 = var8 < 4 ? par4 : (var8 != 12 && var8 != 14 ? par6 : par2);
		return ( (var8 & 1) == 0 ? var9 : -var9) + ( (var8 & 2) == 0 ? var11 : -var11);
	}
	
	/**
	 * pars: noiseArray , xOffset , yOffset , zOffset , xSize , ySize , zSize , xScale, yScale , zScale , noiseScale. noiseArray should be xSize*ySize*zSize in size
	 */
	public void populateNoiseArray(double[] noiseArray, double xValue, double yValue, double zValue, int xSize, int ySize, int zSize, double scaledXScale, double scaledYScale, double scaledZScale, double frequency) {
		int arrayPos;
		int var19;
		int var22;
		double x0;
		double y0;
		double z0;
		int var40;
		int var41;
		double var42;
		int var75;
		
		if (ySize == 1) {
			double var70 = 0.0D;
			double var73 = 0.0D;
			var75 = 0;
			double var77 = 1.0D / frequency;
			
			for (int xPos = 0; xPos < xSize; ++xPos) {
				x0 = xValue + xPos * scaledXScale + this.xCoord;
				int var78 = (int)x0;
				
				if (x0 < var78) {
					--var78;
				}
				
				int var34 = var78 & 255;
				x0 -= var78;
				y0 = qerp(x0);
				
				for (int zPos = 0; zPos < zSize; ++zPos) {
					z0 = zValue + zPos * scaledZScale + this.zCoord;
					var40 = (int)z0;
					
					if (z0 < var40) // floor z0 and store as var40
					{
						--var40;
					}
					
					var41 = var40 & 255;
					z0 -= var40;
					var42 = qerp(z0);
					var19 = this.permutations[var34] + 0; // add zero???
					int var66 = this.permutations[var19] + var41;
					int var67 = this.permutations[var34 + 1] + 0;
					var22 = this.permutations[var67] + var41;
					var70 = this.lerp(y0, this.func_76309_a(this.permutations[var66], x0, z0), this.grad(this.permutations[var22], x0 - 1.0D, 0.0D, z0));
					var73 = this.lerp(y0, this.grad(this.permutations[var66 + 1], x0, 0.0D, z0 - 1.0D), this.grad(this.permutations[var22 + 1], x0 - 1.0D, 0.0D, z0 - 1.0D));
					double var79 = this.lerp(var42, var70, var73);
					arrayPos = var75++;
					noiseArray[arrayPos] += var79 * var77;
				}
			}
		} else {
			var19 = 0;
			double var20 = 1.0D / frequency;
			var22 = -1;
			double var29 = 0.0D;
			x0 = 0.0D;
			double var33 = 0.0D;
			y0 = 0.0D;
			
			for (int xPos = 0; xPos < xSize; ++xPos) {
				z0 = xValue + xPos * scaledXScale + this.xCoord;
				var40 = (int)z0;
				
				if (z0 < var40) {
					--var40;
				}
				
				var41 = var40 & 255;
				z0 -= var40;
				var42 = z0 * z0 * z0 * (z0 * (z0 * 6.0D - 15.0D) + 10.0D);
				
				for (int var44 = 0; var44 < zSize; ++var44) {
					double var45 = zValue + var44 * scaledZScale + this.zCoord;
					int var47 = (int)var45;
					
					if (var45 < var47) {
						--var47;
					}
					
					int var48 = var47 & 255;
					var45 -= var47;
					double var49 = qerp(var45);
					
					for (int var51 = 0; var51 < ySize; ++var51) {
						double var52 = yValue + var51 * scaledYScale + this.yCoord;
						int var54 = (int)var52;
						
						if (var52 < var54) {
							--var54;
						}
						
						int var55 = var54 & 255;
						var52 -= var54;
						double var56 = var52 * var52 * var52 * (var52 * (var52 * 6.0D - 15.0D) + 10.0D);
						
						if (var51 == 0 || var55 != var22) {
							var22 = var55;
							int var69 = this.permutations[var41] + var55;
							int var71 = this.permutations[var69] + var48;
							int var72 = this.permutations[var69 + 1] + var48;
							int var74 = this.permutations[var41 + 1] + var55;
							var75 = this.permutations[var74] + var48;
							int var76 = this.permutations[var74 + 1] + var48;
							var29 = this.lerp(var42, this.grad(this.permutations[var71], z0, var52, var45), this.grad(this.permutations[var75], z0 - 1.0D, var52, var45));
							x0 = this.lerp(var42, this.grad(this.permutations[var72], z0, var52 - 1.0D, var45), this.grad(this.permutations[var76], z0 - 1.0D, var52 - 1.0D, var45));
							var33 = this.lerp(var42, this.grad(this.permutations[var71 + 1], z0, var52, var45 - 1.0D), this.grad(this.permutations[var75 + 1], z0 - 1.0D, var52, var45 - 1.0D));
							y0 = this.lerp(var42, this.grad(this.permutations[var72 + 1], z0, var52 - 1.0D, var45 - 1.0D), this.grad(this.permutations[var76 + 1], z0 - 1.0D, var52 - 1.0D, var45 - 1.0D));
						}
						
						double var58 = this.lerp(var56, var29, x0);
						double var60 = this.lerp(var56, var33, y0);
						double var62 = this.lerp(var49, var58, var60);
						arrayPos = var19++;
						noiseArray[arrayPos] += var62 * var20;
					}
				}
			}
		}
	}
	
	private static double qerp(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}
}
