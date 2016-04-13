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
package cubicchunks.generator.structures;

import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/*
 * Modified Minecraft cave generation code. Based on Robinton's cave generation implementation.
 */

public class CubicCaveGenerator extends CubicStructureGenerator {

	@Override
	protected void generate(World world, Cube cube, int x, int y, int z, int xOrigin, int yOrigin, int zOrigin) {
		if (this.rand.nextInt(16) != 0) {
			return;
		}
		int tries = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);

		if (this.rand.nextInt(7) != 0) {
			tries = 0;
		}

		for (int n1 = 0; n1 < tries; ++n1) {
			double x1 = x * 16 + this.rand.nextInt(16);
			double y1 = y * 16 + this.rand.nextInt(16);
			double z1 = z * 16 + this.rand.nextInt(16);
			int numTries = 1;

			if (this.rand.nextInt(4) == 0) {
				this.generateLargeNode(cube, this.rand.nextLong(), xOrigin, yOrigin, zOrigin, x1, y1, z1);
				numTries += this.rand.nextInt(4);
			}

			for (int n2 = 0; n2 < numTries; ++n2) {
				float curve = this.rand.nextFloat() * (float) Math.PI * 2.0F;
				float angle = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
				float f2 = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();

				if (this.rand.nextInt(10) == 0) {
					f2 *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
				}

				this.generateNode(cube, this.rand.nextLong(), xOrigin, yOrigin, zOrigin, x1, y1, z1, f2, curve,
						angle, 0, 0, 1.0D);
			}
		}
	}

	/**
	 * Generates a larger initial cave node than usual. Called 25% of the time.
	 */
	protected void generateLargeNode(Cube cube, long seed, int xOrigin, int yOrigin, int zOrigin, double x, double y,
			double z) {
		this.generateNode(cube, seed, xOrigin, yOrigin, zOrigin, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F,
				0.0F, -1, -1, 0.5D);
	}

	/**
	 * Generates a node in the current cave system recursion tree.
	 */
	@Override
	protected void generateNode(Cube cube, long seed, int xOrigin, int yOrigin, int zOrigin, double x, double y,
			double z, float size_base, float curve, float angle, int numTry, int tries, double yModSinMultiplier) {
		Random rand = new Random(seed);
		double xOCenter = xOrigin * 16D + 8D;
		double yOCenter = yOrigin * 16D + 8D;
		double zOCenter = zOrigin * 16D + 8D;
		float f3 = 0.0F;
		float f4 = 0.0F;

		if (tries <= 0) {
			int radius = this.range * 16 - 16;
			tries = radius - rand.nextInt(radius / 4);
		}

		boolean kAltered = false;

		if (numTry == -1) {
			numTry = tries / 2;
			kAltered = true;
		}

		int r1 = rand.nextInt(tries / 2) + tries / 4;

		for (; numTry < tries; ++numTry) {
			double modSin = 1.5D + MathHelper.sin(numTry * (float) Math.PI / tries) * size_base * 1.0F;
			double yModSin = modSin * yModSinMultiplier;

			float cosAngle = MathHelper.cos(angle);
			float sinAngle = MathHelper.sin(angle);

			x += MathHelper.cos(curve) * cosAngle;
			y += sinAngle;
			z += MathHelper.sin(curve) * cosAngle;

			boolean r2 = rand.nextInt(6) == 0;

			if (r2) {
				angle *= 0.92F;
			} else {
				angle *= 0.7F;
			}

			angle += f4 * 0.1F;
			curve += f3 * 0.1F;
			f4 *= 0.9F;
			f3 *= 0.75F;
			f4 += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
			f3 += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

			if (!kAltered && numTry == r1 && size_base > 1.0F) {
				this.generateNode(cube, rand.nextLong(), xOrigin, yOrigin, zOrigin, x, y, z,
						rand.nextFloat() * 0.5F + 0.5F, curve - ((float) Math.PI / 2F), angle / 3.0F, numTry, tries,
						1.0D);
				this.generateNode(cube, rand.nextLong(), xOrigin, yOrigin, zOrigin, x, y, z,
						rand.nextFloat() * 0.5F + 0.5F, curve + ((float) Math.PI / 2F), angle / 3.0F, numTry, tries,
						1.0D);
				return;
			}

			if (kAltered || rand.nextInt(4) != 0) {
				double xDist = x - xOCenter;
				double yDist = y - yOCenter;
				double zDist = z - zOCenter;
				double triesLeft = tries - numTry;
				double fDist = size_base + 2.0F + 16.0F;

				// Use yDist?
				if (xDist * xDist + yDist * yDist + zDist * zDist - triesLeft * triesLeft > fDist * fDist) {
					return;
				}

				// Check y coords?
				if (x >= xOCenter - 16.0D - modSin * 2.0D && y >= yOCenter - 16.0D - yModSin * 2.0D
						&& z >= zOCenter - 16.0D - modSin * 2.0D && x <= xOCenter + 16.0D + modSin * 2.0D
						&& y <= yOCenter + 16.0D + yModSin * 2.0D && z <= zOCenter + 16.0D + modSin * 2.0D) {
					int xDist1 = MathHelper.floor_double(x - modSin) - xOrigin * 16 - 1;
					int xDist2 = MathHelper.floor_double(x + modSin) - xOrigin * 16 + 1;
					int yDist1 = MathHelper.floor_double(y - yModSin) - yOrigin * 16 - 1;
					int yDist2 = MathHelper.floor_double(y + yModSin) - yOrigin * 16 + 1;
					int zDist1 = MathHelper.floor_double(z - modSin) - zOrigin * 16 - 1;
					int zDist2 = MathHelper.floor_double(z + modSin) - zOrigin * 16 + 1;

					// Probably this causes some glitches
					if (xDist1 < 0) {
						xDist1 = 0;
					}

					if (xDist2 > 16) {
						xDist2 = 16;
					}

					if (yDist1 < 0)// orig: 1
					{
						yDist1 = 0;// orig: 1
					}

					if (yDist2 > 16)// orig: 120
					{
						yDist2 = 16;// orig: 120
					}

					if (zDist1 < 0) {
						zDist1 = 0;
					}

					if (zDist2 > 16) {
						zDist2 = 16;
					}

					boolean hitLiquid = scanForLiquid(cube, xDist1, xDist2, yDist1, yDist2, zDist1, zDist2, Blocks.LAVA, Blocks.FLOWING_LAVA);
					
					if (!hitLiquid) {
						for (int x1 = xDist1; x1 < xDist2; ++x1) {
							double distX = calculateDistance(xOrigin, x1, x, modSin);

							for (int z1 = zDist1; z1 < zDist2; ++z1) {
								double distZ = calculateDistance(zOrigin, z1, z, modSin);
								boolean grass = false;

								// scan from top to bottom, so we can find grass and move it down to replace dirt
								for (int y1 = yDist2 - 1; y1 >= yDist1; --y1) {
									double distY = calculateDistance(yOrigin, y1, y, yModSin);

									BlockPos pos = new BlockPos(x1, y1, z1);
									Block block = cube.getBlockState(pos).getBlock();

									if (block != Blocks.STONE && block != Blocks.DIRT && block != Blocks.GRASS) {
										continue;
									} else if (block == Blocks.GRASS) {
										grass = true;
									}

									if (distY > -0.7D && distX * distX + distY * distY + distZ * distZ < 1.0D) {
										// No lava generation, infinite depth. Lava will be generated differently (or
										// not generated)
										cube.setBlockForGeneration(pos, Blocks.AIR.getDefaultState());
									}
									
									if (grass && block == Blocks.DIRT) {
										cube.setBlockForGeneration(pos, Blocks.GRASS.getDefaultState());
										cube.setBlockForGeneration(pos.up(), Blocks.AIR.getDefaultState());
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
}
