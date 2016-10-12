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
package cubicchunks.worldgen.generator.custom.structures;

import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class CubicRavineGenerator extends CubicStructureGenerator {

	// I'm not sure what it is
	private float[] array1 = new float[1024];

	@Override
	protected void generate(ICubicWorld world, ICubePrimer cube, int cubeX, int cubeY, int cubeZ, int xOrigin, int yOrigin,
	                        int zOrigin) {
		if (rand.nextInt(16) != 0) {
			return;
		}
		if (cubeY <= 4 && rand.nextInt(50) == 0) {
			double x = cubeX*16 + rand.nextInt(16);
			double y = cubeY*16 + rand.nextInt(16);
			double z = cubeZ*16 + rand.nextInt(16);
			byte numGen = 1;

			for (int i = 0; i < numGen; ++i) {
				float curve = rand.nextFloat()*(float) Math.PI*2.0F;
				float angle = (rand.nextFloat() - 0.5F)*2.0F/8.0F;
				float f = (rand.nextFloat()*2.0F + rand.nextFloat())*2.0F;
				this.generateNode(cube, rand.nextLong(), xOrigin, yOrigin, zOrigin, x, y, z, f, curve, angle, 0, 0,
						3.0D);
			}
		}
	}

	@Override
	protected void generateNode(ICubePrimer cube, long seed, int xOrigin, int yOrigin, int zOrigin, double x, double y,
	                            double z, float size_base, float curve, float angle, int numTry, int tries, double yModSinMultiplier) {
		Random rand = new Random(seed);
		double xOCenter = xOrigin*16 + 8;
		double yOCenter = yOrigin*16 + 8;
		double zOCenter = zOrigin*16 + 8;
		float f3 = 0.0F;
		float f4 = 0.0F;

		if (tries <= 0) {
			int radius = range*16 - 16;
			tries = radius - rand.nextInt(radius/4);
		}

		boolean kAltered = false;

		if (numTry == -1) {
			numTry = tries/2;
			kAltered = true;
		}

		this.array1 = populateArray(rand);

		for (; numTry < tries; ++numTry) {
			double modSin = 1.5D + MathHelper.sin(numTry*(float) Math.PI/tries)*size_base*1.0F;
			double yModSin = modSin*yModSinMultiplier;

			float cosAngle = MathHelper.cos(angle);
			float sinAngle = MathHelper.sin(angle);

			modSin *= rand.nextFloat()*0.25D + 0.75D;// * value between 0.75
			// and 1
			yModSin *= rand.nextFloat()*0.25D + 0.75D;

			x += MathHelper.cos(curve)*cosAngle;
			y += sinAngle;
			z += MathHelper.sin(curve)*cosAngle;

			angle *= 0.7F;

			angle += f4*0.05F;
			curve += f3*0.05F;
			f4 *= 0.8F;
			f3 *= 0.5F;
			f4 += (rand.nextFloat() - rand.nextFloat())*rand.nextFloat()*2.0F;
			f3 += (rand.nextFloat() - rand.nextFloat())*rand.nextFloat()*4.0F;

			if (kAltered || rand.nextInt(4) != 0) {
				double xDist = x - xOCenter;
				// double yDist = y - yOCenter;
				double zDist = z - zOCenter;
				double triesLeft = tries - numTry;
				double fDist = size_base + 2.0F + 16.0F;

				if (xDist*xDist + zDist*zDist - triesLeft*triesLeft > fDist*fDist) {
					return;
				}

				if (x >= xOCenter - 16.0D - modSin*2.0D && y >= yOCenter - 16.0D - yModSin*2.0D
						&& z >= zOCenter - 16.0D - modSin*2.0D && x <= xOCenter + 16.0D + modSin*2.0D
						&& y <= yOCenter + 16.0D + yModSin*2.0D && z <= zOCenter + 16.0D + modSin*2.0D) {
					int xDist1 = MathHelper.floor_double(x - modSin) - xOrigin*16 - 1;
					int xDist2 = MathHelper.floor_double(x + modSin) - xOrigin*16 + 1;
					int yDist1 = MathHelper.floor_double(y - yModSin) - yOrigin*16 - 1;
					int yDist2 = MathHelper.floor_double(y + yModSin) - yOrigin*16 + 1;
					int zDist1 = MathHelper.floor_double(z - modSin) - zOrigin*16 - 1;
					int zDist2 = MathHelper.floor_double(z + modSin) - zOrigin*16 + 1;

					if (xDist1 < 0) {
						xDist1 = 0;
					}

					if (xDist2 > 16) {
						xDist2 = 16;
					}

					if (yDist1 < 0)// 1
					{
						yDist1 = 0;// 1
					}

					if (yDist2 > 16)// 120
					{
						yDist2 = 16;// 120
					}

					if (zDist1 < 0) {
						zDist1 = 0;
					}

					if (zDist2 > 16) {
						zDist2 = 16;
					}

					boolean hitLiquid = scanForLiquid(cube, xDist1, xDist2, yDist1, yDist2, zDist1, zDist2, Blocks.WATER, Blocks.FLOWING_WATER);

					if (!hitLiquid) {
						for (int x1 = xDist1; x1 < xDist2; ++x1) {
							double distX = calculateDistance(xOrigin, x1, x, modSin);

							for (int z1 = zDist1; z1 < zDist2; ++z1) {
								double distZ = calculateDistance(zOrigin, z1, z, modSin);
								boolean grass = false;

								if (distX*distX + distZ*distZ >= 1.0D) {
									continue;
								}
								for (int y1 = yDist2 - 1; y1 >= yDist1; --y1) {
									double distY = calculateDistance(yOrigin, y1, y, yModSin);

									if ((distX*distX + distZ*distZ)*this.array1[(y1 + yOrigin*16) & 0xFF] + distY
											*distY/6.0D >= 1.0D) {
										continue;
									}

									Block block = cube.getBlockState(x1, y1, z1).getBlock();

									if (block != Blocks.STONE && block != Blocks.DIRT && block != Blocks.GRASS) {
										continue;
									} else if (block == Blocks.GRASS) {
										grass = true;
									}
									// used to place lava at the bottom of ravines if it was deep enough
									if (y1 + yOrigin*16 < /* 10 */0) {
										// BUG: crash when it's lava
										// cube.setBlockForGeneration(pos, Blocks.FLOWING_LAVA.getDefaultState());
										cube.setBlockState(x1, y1, z1, Blocks.AIR.getDefaultState());
									} else {
										cube.setBlockState(x1, y1, z1, Blocks.AIR.getDefaultState());
									}

									if (grass && block == Blocks.DIRT) {
										cube.setBlockState(x1, y1, z1, Blocks.GRASS.getDefaultState());
										cube.setBlockState(x1, y1 + 1, z1, Blocks.AIR.getDefaultState());
									}
								}
							}
						}
					}

					if (kAltered) {
						break;
					}
				}
			}
		}
	}

	private float[] populateArray(Random rand) {
		float[] result = new float[1024];
		float value = 1.0F;

		for (int i = 0; i < 256; ++i) {
			if (i == 0 || rand.nextInt(3) == 0) {
				value = 1.0F + rand.nextFloat()*rand.nextFloat();// * 1.0F; // 1.X, lower = higher probability
			}

			result[i] = value*value;
		}

		return result;
	}
}
