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
package cubicchunks.worldgen.generator.custom.builder;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.Arrays;

import cubicchunks.util.Coords;
import cubicchunks.world.cube.Cube;

// a small hack to get biome generation working with the new system
// todo: make it not hacky
public class BiomeHeightVolatilitySource {
	private static final double INVALID = Double.MAX_VALUE;
	private final double[] nearBiomeWeightArray;

	private BiomeProvider biomeGen;
	private final int smoothRadius;
	private final int smoothDiameter;
	private final int xSection;
	private final int zSection;

	private Biome[] biomesToInterpolate;
	private Biome[] biomesBlockScale;

	private double[][] cachedVolatility;
	private double[][] cachedHeight;
	private int chunkX;
	private int chunkZ;


	public BiomeHeightVolatilitySource(BiomeProvider biomeGen, int smoothRadius, int xSection, int zSection) {
		this.biomeGen = biomeGen;
		this.smoothRadius = smoothRadius;
		this.smoothDiameter = smoothRadius*2 + 1;
		this.xSection = xSection;
		this.zSection = zSection;
		this.biomesToInterpolate = null;

		this.nearBiomeWeightArray = new double[this.smoothDiameter*this.smoothDiameter];

		for (int x = -this.smoothRadius; x <= this.smoothRadius; x++) {
			for (int z = -this.smoothRadius; z <= this.smoothRadius; z++) {
				final double val = 10.0F/Math.sqrt(x*x + z*z + 0.2F);
				this.nearBiomeWeightArray[x + this.smoothRadius + (z + this.smoothRadius)*this.smoothDiameter] = val;
			}
		}

		cachedVolatility = new double[xSection][zSection];
		cachedHeight = new double[xSection][zSection];
	}

	public void setChunk(int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		biomesToInterpolate = biomeGen.getBiomesForGeneration(this.biomesToInterpolate,
			chunkX*(xSection - 1) - this.smoothRadius,
			chunkZ*(zSection - 1) - this.smoothRadius,
			xSection + smoothDiameter, zSection + smoothDiameter);

		biomesBlockScale = biomeGen.getBiomes(
			this.biomesBlockScale,
			Coords.cubeToMinBlock(chunkX),
			Coords.cubeToMinBlock(chunkZ),
			Cube.SIZE, Cube.SIZE);

		for (double[] d : cachedHeight)
			Arrays.fill(d, INVALID);
		for (double[] d : cachedVolatility)
			Arrays.fill(d, INVALID);
	}

	public double getHeight(int x, int y, int z) {
		return get(x, z, cachedHeight);
	}

	public double getVolatility(int x, int y, int z) {
		return get(x, z, cachedVolatility);
	}

	public Biome getBiome(int blockX, int blockY, int blockZ) {
		return biomesBlockScale[Coords.blockToLocal(blockZ) << 4 | Coords.blockToLocal(blockX)];
	}

	private double get(int x, int z, double[][] array) {
		int localX = x - chunkX*(xSection - 1);
		int localZ = z - chunkZ*(zSection - 1);
		if (array[localX][localZ] != INVALID) {
			return array[localX][localZ];
		}
		updateCached(localX, localZ);

		return array[localX][localZ];
	}

	private void updateCached(int x, int z) {

		// Calculate weighted average of nearby biomes height and volatility
		double smoothVolatility = 0.0F;
		double smoothHeight = 0.0F;

		double biomeWeightSum = 0.0F;
		final Biome centerBiomeConfig = getCenterBiome(x, z);
		final int lookRadius = this.smoothRadius;

		for (int nextX = -lookRadius; nextX <= lookRadius; nextX++) {
			for (int nextZ = -lookRadius; nextZ <= lookRadius; nextZ++) {
				final Biome biome = getOffsetBiome(x, z, nextX, nextZ);
				final double biomeHeight = biome.getBaseHeight();
				final double biomeVolatility = biome.getHeightVariation();

				double biomeWeight = calcBiomeWeight(nextX, nextZ, biomeHeight);

				biomeWeight = Math.abs(biomeWeight);
				if (biomeHeight > centerBiomeConfig.getBaseHeight()) {
					// prefer biomes with lower height?
					biomeWeight /= 2.0F;
				}
				smoothVolatility += biomeVolatility*biomeWeight;
				smoothHeight += biomeHeight*biomeWeight;

				biomeWeightSum += biomeWeight;
			}
		}

		smoothVolatility /= biomeWeightSum;
		smoothHeight /= biomeWeightSum;

		// Convert from vanilla height/volatility format
		// to something easier to predict
		smoothVolatility = smoothVolatility*0.9 + 0.1;
		this.cachedVolatility[x][z] = smoothVolatility*4.0/3.0;

		// divide everything by 64, then it will be multpllied by maxElev
		// vanilla sea level: 63.75 / 64.00

		// sea level 0.75/64 of height above sea level (63.75 = 63+0.75)
		this.cachedHeight[x][z] = 0.75/64.0;
		this.cachedHeight[x][z] += smoothHeight*17.0/64.0;
	}


	private Biome getCenterBiome(final int x, final int z) {
		return this.biomesToInterpolate[x + this.smoothRadius + (z + this.smoothRadius)*(xSection + this.smoothDiameter)];
	}

	private Biome getOffsetBiome(final int x, final int z, int nextX, int nextZ) {
		return this.biomesToInterpolate[x + nextX + this.smoothRadius + (z + nextZ + this.smoothRadius)*(xSection + this.smoothDiameter)];
	}

	private double calcBiomeWeight(int nextX, int nextZ, double biomeHeight) {
		return this.nearBiomeWeightArray[nextX + this.smoothRadius + (nextZ + this.smoothRadius)*this.smoothDiameter]/(biomeHeight + 2.0F);
	}
}
