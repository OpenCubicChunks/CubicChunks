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
package cubicchunks.worldgen.generator.custom;

import cubicchunks.api.generators.ITerrainGenerator;
import cubicchunks.worldgen.generator.custom.builder.BasicBuilder;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Random;

import static cubicchunks.worldgen.generator.GlobalGeneratorConfig.*;
import static cubicchunks.util.MathUtil.lerp;
import static cubicchunks.util.TerrainGeneratorUtils.*;

public class CustomTerrainGenerator implements ITerrainGenerator {
	private static final int OCTAVES = 16;

	private BiomeGenBase[] biomes;

	private final long seed;
	private final Random rand;

	private final double[][][] noiseArrayHigh;
	private final double[][][] noiseArrayLow;
	private final double[][][] noiseArrayAlpha;

	private final double[][][] rawDensity;

	private final IBuilder builderHigh;
	private final IBuilder builderLow;
	private final IBuilder builderAlpha;

	private double biomeVolatility;
	private double biomeHeight;

	private final int maxSmoothRadius;
	private final int maxSmoothDiameter;

	private final double[][] noiseArrayHeight;

	private final double[] nearBiomeWeightArray;

	private final BasicBuilder builderHeight;

	private final boolean needsScaling = true;

	public CustomTerrainGenerator(final long seed) {

		this.seed = seed;
		this.rand = new Random(seed);

		this.maxSmoothRadius = 2 * (int) (MAX_ELEV / 64);
		this.maxSmoothDiameter = this.maxSmoothRadius * 2 + 1;

		this.biomes = null;

		this.noiseArrayHigh = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];
		this.noiseArrayLow = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];
		this.noiseArrayAlpha = new double[X_SECTIONS][Y_SECTIONS][Z_SECTIONS];

		this.rawDensity = getNewCubeSizedArray();

		this.builderHigh = createHighBuilder();
		this.builderLow = createLowBuilder();
		this.builderAlpha = createAlphaBuilder();

		this.noiseArrayHeight = new double[X_SECTIONS][Z_SECTIONS];

		this.nearBiomeWeightArray = new double[this.maxSmoothDiameter * this.maxSmoothDiameter];

		for (int x = -this.maxSmoothRadius; x <= this.maxSmoothRadius; x++) {
			for (int z = -this.maxSmoothRadius; z <= this.maxSmoothRadius; z++) {
				final double f1 = 10.0F / Math.sqrt(x * x + z * z + 0.2F);
				this.nearBiomeWeightArray[x + this.maxSmoothRadius + (z + this.maxSmoothRadius)
						* this.maxSmoothDiameter] = f1;
			}
		}

		double freq = 200.0 / Math.pow(2, 10) / (MAX_ELEV / 64);

		this.builderHeight = new BasicBuilder();
		this.builderHeight.setSeed(this.rand.nextInt());
		this.builderHeight.setOctaves(10);
		this.builderHeight.setMaxElev(8);
		this.builderHeight.setFreq(freq);
		this.builderHeight.build();
	}

	@Override
	public double[][][] generate(final Cube cube) {
		generateNoiseArrays(cube);

		generateTerrainArray(cube);

		if (this.needsScaling) {
			scaleNoiseArray(cube.getWorld().provider.getAverageGroundLevel());
		}

		return applyHeightGradient(cube, expandNoiseArray(this.rawDensity));
	}

	/**
	 * if rawDensity is ranged from -1 to 1, use this to scale it up
	 */
	private final void scaleNoiseArray(final int seaLevel) {
		for (int x = 0; x < X_SECTIONS; x++) {
			for (int z = 0; z < Z_SECTIONS; z++) {
				for (int y = 0; y < Y_SECTIONS; y++) {
					this.rawDensity[x][y][z] *= MAX_ELEV;
					this.rawDensity[x][y][z] += seaLevel;
				}
			}
		}
	}

	private IBuilder createHighBuilder() {
		Random rand = new Random(this.seed * 2);
		double freq = 684.412D / Math.pow(2, OCTAVES) / (MAX_ELEV / 64.0);

		BasicBuilder builderHigh = new BasicBuilder();
		builderHigh.setSeed(rand.nextInt());
		builderHigh.setOctaves(OCTAVES);
		builderHigh.setPersistance(0.5);
		// with 16 octaves probability of getting 1 is too low
		builderHigh.setMaxElev(2);
		builderHigh.setClamp(-1, 1);
		builderHigh.setFreq(freq, freq, freq);
		builderHigh.build();

		return builderHigh;
	}

	private IBuilder createLowBuilder() {
		Random rand = new Random(this.seed * 3);
		double freq = 684.412D / Math.pow(2, OCTAVES) / (MAX_ELEV / 64.0);

		BasicBuilder builderLow = new BasicBuilder();
		builderLow.setSeed(rand.nextInt());
		builderLow.setOctaves(OCTAVES);
		builderLow.setPersistance(0.5);
		builderLow.setMaxElev(2);
		builderLow.setClamp(-1, 1);
		builderLow.setFreq(freq, freq, freq);
		builderLow.build();

		return builderLow;
	}

	private IBuilder createAlphaBuilder() {
		Random rand = new Random(this.seed * 4);
		double freq = 8.55515 / Math.pow(2, 8) / (MAX_ELEV / 64.0);

		BasicBuilder builderAlpha = new BasicBuilder();
		builderAlpha.setSeed(rand.nextInt());
		builderAlpha.setOctaves(8);
		builderAlpha.setPersistance(0.5);
		builderAlpha.setMaxElev(25.6);
		builderAlpha.setSeaLevel(0.5);
		builderAlpha.setClamp(0, 1);
		builderAlpha.setFreq(freq, freq * 2, freq);
		builderAlpha.build();

		return builderAlpha;
	}

	/*
	 * (non-Javadoc)
	 * @see cubicchunks.worldgen.generator.ITerrainGenerator#generateNoiseArrays(cubicchunks.world.cube.Cube)
	 */
	private void generateNoiseArrays(final Cube cube) {
		int cubeXMin = cube.getX() * (X_SECTIONS - 1);
		int cubeYMin = cube.getY() * (Y_SECTIONS - 1);
		int cubeZMin = cube.getZ() * (Z_SECTIONS - 1);

		for (int x = 0; x < X_SECTIONS; x++) {
			int xPos = cubeXMin + x;

			for (int z = 0; z < Z_SECTIONS; z++) {
				int zPos = cubeZMin + z;

				for (int y = 0; y < Y_SECTIONS; y++) {
					int yPos = cubeYMin + y;

					this.noiseArrayHigh[x][y][z] = this.builderHigh.getValue(xPos, yPos, zPos);
					this.noiseArrayLow[x][y][z] = this.builderLow.getValue(xPos, yPos, zPos);
					this.noiseArrayAlpha[x][y][z] = this.builderAlpha.getValue(xPos, yPos, zPos);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see cubicchunks.worldgen.generator.ITerrainGenerator#generateTerrainArray(cubicchunks.world.cube.Cube)
	 */
	private void generateTerrainArray(final Cube cube) {
		this.biomes = getBiomeMap(cube);

		fillHeightArray(cube);
		for (int x = 0; x < X_SECTIONS; x++) {
			for (int z = 0; z < Z_SECTIONS; z++) {
				// TODO: Remove addHeight?
				double addHeight = getAddHeight(x, z);
				biomeFactor(x, z, addHeight);

				for (int y = 0; y < Y_SECTIONS; y++) {
					final double vol1Low = this.noiseArrayLow[x][y][z];
					final double vol2High = this.noiseArrayHigh[x][y][z];

					final double noiseAlpha = this.noiseArrayAlpha[x][y][z];

					double output = lerp(noiseAlpha, vol1Low, vol2High);

					double heightModifier = this.biomeHeight;
					double volatilityModifier = this.biomeVolatility;

					final double yAbs = (cube.getY() * 16.0 + y * 8.0) / MAX_ELEV;
					if (yAbs < heightModifier) {
						// generator below average biome geight is more flat
						volatilityModifier /= 4.0;
					}

					// NOTE: Multiplication by nonnegative number and addition when using 3d noise effects are the same
					// as with heightmap.

					// make height range lower
					output *= volatilityModifier;
					// height shift
					output += heightModifier;

					// Since in TWM we don't have height limit we could skip it but PLATEAU biomes need it
					int maxYSections = (int) Math.round(MAX_ELEV / Y_SECTION_SIZE);
					if (yAbs * MAX_ELEV > maxYSections - 4) {
						// TODO: Convert Y cutoff to work correctly with noise between -1 and 1
						// final double a = ( yAbs - ( maxYSections - 4 ) ) / 3.0F;
						// output = output * ( 1.0D - a ) - 10.0D * a;
					}
					this.rawDensity[x][y][z] = output;
				}
			}
		}
	}

	private BiomeGenBase[] getBiomeMap(final Cube cube) {
		return cube.getWorld().provider.getBiomeProvider().getBiomesForGeneration(this.biomes,
				cube.getX() * 4 - this.maxSmoothRadius, cube.getZ() * 4 - this.maxSmoothRadius,
				X_SECTION_SIZE + this.maxSmoothDiameter, Z_SECTION_SIZE + this.maxSmoothDiameter);
	}

	/**
	 * Calculates biome height and volatility and adds addHeight to result.
	 * <p>
	 * It converts vanilla biome values to some more predictable format:
	 * <p>
	 * biome volatility == 0 will generate flat generator
	 * <p>
	 * biome volatility == 0.5 means that max difference between the actual height and average height is 0.5 of max
	 * generation height from sea level. High volatility will generate overhangs
	 * <p>
	 * biome height == 0 will generate generator at sea level
	 * <p>
	 * biome height == 1 will generate generator will generate at max generation height above sea level.
	 * <p>
	 * Volatility Note: Terrain below biome height has volatility divided by 4, probably to add some flat generator to
	 * mountanious biomes
	 */
	private void biomeFactor(final int x, final int z, final double addHeight) {
		// Calculate weighted average of nearby biomes height and volatility
		float smoothVolatility = 0.0F;
		float smoothHeight = 0.0F;

		float biomeWeightSum = 0.0F;
		final BiomeGenBase centerBiomeConfig = getCenterBiome(x, z);
		final int lookRadius = this.maxSmoothRadius;

		for (int nextX = -lookRadius; nextX <= lookRadius; nextX++) {
			for (int nextZ = -lookRadius; nextZ <= lookRadius; nextZ++) {
				final BiomeGenBase biome = getOffsetBiome(x, z, nextX, nextZ);
				final float biomeHeight = biome.getBaseHeight();
				final float biomeVolatility = biome.getHeightVariation();

				double biomeWeight = calcBiomeWeight(nextX, nextZ, biomeHeight);

				biomeWeight = Math.abs(biomeWeight);
				if (biomeHeight > centerBiomeConfig.getBaseHeight()) {
					// prefer biomes with lower height?
					biomeWeight /= 2.0F;
				}
				smoothVolatility += biomeVolatility * biomeWeight;
				smoothHeight += biomeHeight * biomeWeight;

				biomeWeightSum += biomeWeight;
			}
		}

		smoothVolatility /= biomeWeightSum;
		smoothHeight /= biomeWeightSum;

		// Convert from vanilla height/volatility format
		// to something easier to predict
		this.biomeVolatility = smoothVolatility * 0.9 + 0.1;
		this.biomeVolatility *= 4.0 / 3.0;

		// divide everything by 64, then it will be multpllied by maxElev
		// vanilla sea level: 63.75 / 64.00

		// sea level 0.75/64 of height above sea level (63.75 = 63+0.75)
		this.biomeHeight = 0.75 / 64.0;
		this.biomeHeight += smoothHeight * 17.0 / 64.0;
		// TODO: Remove addHeight? it changes the result by at most 1 block
		this.biomeHeight += 0.2 * addHeight * 17.0 / 64.0;
	}

	private BiomeGenBase getCenterBiome(final int x, final int z) {
		return this.biomes[x + this.maxSmoothRadius + (z + this.maxSmoothRadius)
				* (X_SECTION_SIZE + this.maxSmoothDiameter)];
	}

	private BiomeGenBase getOffsetBiome(final int x, final int z, int nextX, int nextZ) {
		return this.biomes[x + nextX + this.maxSmoothRadius + (z + nextZ + this.maxSmoothRadius)
				* (X_SECTION_SIZE + this.maxSmoothDiameter)];
	}

	private double calcBiomeWeight(int nextX, int nextZ, float biomeHeight) {
		return this.nearBiomeWeightArray[nextX + this.maxSmoothRadius + (nextZ + this.maxSmoothRadius)
				* this.maxSmoothDiameter]
				/ (biomeHeight + 2.0F);
	}

	private void fillHeightArray(final Cube cube) {
		int cubeXMin = cube.getX() * (X_SECTION_SIZE - 1);
		int cubeZMin = cube.getZ() * (Z_SECTION_SIZE - 1);

		for (int x = 0; x < X_SECTIONS; x++) {
			int xPos = cubeXMin + x;

			for (int z = 0; z < Z_SECTIONS; z++) {
				int zPos = cubeZMin + z;

				this.noiseArrayHeight[x][z] = this.builderHeight.getValue(xPos, 0, zPos);

			}
		}
	}

	/**
	 * This method is there only because the code exists in vanilla, it affects generator height by at most 1 block
	 * (+/-0.425 blocks). In Minecraft beta it was base generator height, but as of beta 1.8 it doesn't have any
	 * significant effect. It's multiplied 0.2 before it's used.
	 */
	private double getAddHeight(final int x, final int z) {
		double noiseHeight = this.noiseArrayHeight[x][z];

		assert noiseHeight <= 8 && noiseHeight >= -8;

		if (noiseHeight < 0.0D) {
			noiseHeight = -noiseHeight * 0.3D;
		}

		noiseHeight = noiseHeight * 3.0D - 2.0D;

		if (noiseHeight < 0.0D) {
			noiseHeight /= 2.0D;

			if (noiseHeight < -1.0D) {
				noiseHeight = -1.0D;
			}
			noiseHeight /= 1.4D;
			noiseHeight /= 2.0D;

		} else {
			if (noiseHeight > 1.0D) {
				noiseHeight = 1.0D;
			}
			noiseHeight /= 8.0D;
		}
		return noiseHeight;
	}
}
