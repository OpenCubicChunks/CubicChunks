/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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
package cubicchunks.generator.biome.alternateGen;

import cubicchunks.cache.CacheMap;
import cubicchunks.generator.biome.biomegen.CCBiome;
import cubicchunks.server.CubeWorldServer;

public class NewAlternateWorldColumnManager extends AlternateWorldColumnManager {

	// Cache for noise fields accessed only once every 64 columns
	private final CacheMap<Long, NoiseArrays> noiseCache2 = new CacheMap<Long, NoiseArrays>(128, 132);

	private int zoom = 7;

	public NewAlternateWorldColumnManager(CubeWorldServer world) {
		super(world);
	}

	protected void generateBiomes(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length) {
		int minChunkX = blockX >> 4;
		int minChunkZ = blockZ >> 4;
		int maxChunkX = (blockX + width - 1) >> 4;
		int maxChunkZ = (blockZ + length - 1) >> 4;

		assert minChunkX <= maxChunkX;
		assert minChunkZ <= maxChunkZ;

		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				double[][] heightArray = this.getHeightArray(x, z);
				double[][] volArray = getVolArray(x, z);
				double[][] tempArray = getTempArray(x, z);
				double[][] rainfallArray = getRainfallArray(x, z);
				for (int xRel = 0; xRel < 16; xRel++) {
					for (int zRel = 0; zRel < 16; zRel++) {
						int[] coords = getAlteredCoordinates((x << 4) + xRel, (z << 4) + zRel, this.worldSeed);
						double height;
						double vol;
						double rainfall;
						double temp;
						CCBiome biome;
						if (heightArray[xRel][zRel] > 0.05) {
							int x1 = coords[0];
							int z1 = coords[1];
							int chunkX = x1 >> 4;
							int chunkZ = z1 >> 4;
							vol = getVolFromCache2(x1, z1);
							height = getHeightFromCache2(x1, z1);
							temp = getTempFromCache2(x1, z1);
							rainfall = getRainfallFromCache2(x1, z1);
							height = height > 1 ? 1 : height;
							height = height < -1 ? -1 : height;
							vol = getRealVolatility(vol, height, rainfall, temp);
							height = (height < 0.05) ? 0.05 : height;
							biome = getBiomeForValues(chunkX, chunkZ, vol, height, temp, rainfall);
						} else {
							vol = volArray[xRel][zRel];
							height = heightArray[xRel][zRel];
							temp = tempArray[xRel][zRel];
							rainfall = rainfallArray[xRel][zRel];
							height = height > 1 ? 1 : height;
							height = height < -1 ? -1 : height;
							vol = getRealVolatility(vol / 2, height, rainfall, temp);

							biome = getBiomeForValues(x, z, vol, height, temp, rainfall);
						}

						// height = getRealHeight( height );

						biomes[zRel * length + xRel] = biome;
					}
				}
			}
		}
	}

	public int[] getAlteredCoordinates(int x, int z, Long seed) {
		int zoom = this.zoom;
		long orgseed = seed;
		int[] array1 = new int[] { 1, 2, 3, 4 };
		int[] array2 = new int[] { 0, 0, 0, 0, 0, 0 };
		for (int i = zoom; i >= 0; i--) {
			seed = initChunkSeed(x >> (i + 1), z >> (i + 1), orgseed + i);
			array2[0] = array1[0];
			array2[1] = array1[this.nextInt(2, seed)];
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
			seed += this.worldSeed;
			array2[2] = array1[this.nextInt(2, seed) << 1];
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
			seed += this.worldSeed;
			array2[3] = array1[0] == array1[1] && array1[0] == array1[2] ? array1[0] : (array1[1] == array1[2]
					&& array1[1] == array1[3] ? array1[1] : ((array1[0] == array1[1] && array1[2] != array1[3])
					|| (array1[0] == array1[2] && array1[1] != array1[3])
					|| (array1[0] == array1[3] && array1[1] != array1[2]) ? array1[0]
					: ((array1[1] == array1[2] && array1[0] != array1[3])
							|| (array1[1] == array1[2] && array1[0] != array1[3]) ? array1[1] : array1[2] == array1[3]
							&& array1[0] != array1[1] ? array1[2] : array1[this.nextInt(4, seed)])));
			seed = initChunkSeed(x >> (i + 1), (z >> (i + 1)) + 1, orgseed + i);
			array2[5] = array1[this.nextInt(2, seed) + 2];
			seed = initChunkSeed((x >> (i + 1)) + 1, z >> (i + 1), orgseed + i);
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
			seed += this.worldSeed;
			array2[4] = array1[(this.nextInt(2, seed) << 1) + 1];
			int q1 = (x >> i) & 1;
			int q2 = (z >> i) & 1;
			assert ((q1 == 0 || q1 == 1) && (q2 == 0 || q2 == 1));
			if (q1 == 0 && q2 == 0) {
				array1[1] = array2[1];
				array1[2] = array2[2];
				array1[3] = array2[3];
			} else if (q1 == 1 && q2 == 0) {
				array1[0] = array2[1];
				array1[2] = array2[3];
				array1[3] = array2[4];
			} else if (q1 == 0 && q2 == 1) {
				array1[0] = array2[2];
				array1[1] = array2[3];
				array1[3] = array2[5];
			} else {
				array1[0] = array2[3];
				array1[1] = array2[4];
				array1[2] = array2[5];
			}
			if (array1[0] == array1[1] && array1[0] == array1[2] && array1[0] == array1[3]) {
				break;
			}
		}
		zoom = zoom + 1;
		if (array2[3] == 1) {
			x = (x >> zoom) << zoom;
			z = (z >> zoom) << zoom;
		} else if (array2[3] == 2) {
			x = ((x >> zoom) + 1) << zoom;
			z = (z >> zoom) << zoom;
		} else if (array2[3] == 3) {
			x = (x >> zoom) << zoom;
			z = ((z >> zoom) + 1) << zoom;
		} else {
			x = ((x >> zoom) + 1) << zoom;
			z = ((z >> zoom) + 1) << zoom;
		}

		return new int[] { x, z };
	}

	/**
	 * Initialize layer's current chunkSeed based on the local worldGenSeed and
	 * the (x,z) chunk coordinates.
	 */
	public long initChunkSeed(long x, long z, long seed) {
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		seed += x;
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		seed += z;
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		seed += x;
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		seed += z;
		return seed;
	}

	/**
	 * returns a LCG pseudo random number from [0, x). Args: int x
	 */
	protected int nextInt(int par1, Long seed) {
		int var2 = (int) ((seed >> 24) % (long) par1);

		if (var2 < 0) {
			var2 += par1;
		}
		return var2;
	}

	public double getTempFromCache2(int X, int Z) {
		return getFromCache2OrGenerate(noiseCache2, NoiseArrays.Type.TEMPERATURE, X, Z)[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Rainfall array
	 */
	public double getRainfallFromCache2(int X, int Z) {
		return getFromCache2OrGenerate(noiseCache2, NoiseArrays.Type.RAINFALL, X, Z)[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Volatility array
	 */
	public double getVolFromCache2(int X, int Z) {
		return getFromCache2OrGenerate(noiseCache2, NoiseArrays.Type.VOLATILITY, X, Z)[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Height array, needs to be interpolated (4x4 by default)
	 */
	public double getHeightFromCache2(int X, int Z) {
		return getFromCache2OrGenerate(noiseCache2, NoiseArrays.Type.HEIGHT, X, Z)[0][0];
	}

	private double[][] getFromCache2OrGenerate(CacheMap<Long, NoiseArrays> cache, NoiseArrays.Type type, int X, int Z) {
		X = (X >> zoom) << zoom;
		Z = (Z >> zoom) << zoom;
		NoiseArrays arrays = cache.get(xzToLong(X, Z));
		if (arrays == null) {
			addToNoiseCache2(X, Z);
			arrays = cache.get(xzToLong(X, Z));
			assert arrays != null;
		}
		return arrays.get(type);
	}

	private void addToNoiseCache2(int X, int Z) {
		double[][] vol = new double[1][1];
		double[][] height = new double[1][1];
		double[][] temp = new double[1][1];
		double[][] rainfall = new double[1][1];
		vol[0][0] = this.getVolatility(X, Z);
		height[0][0] = this.getHeight(X, Z);
		temp[0][0] = this.getTemp(X, Z);
		rainfall[0][0] = this.getRainfall(X, Z);

		NoiseArrays arrays2 = new NoiseArrays(vol, height, temp, rainfall);
		noiseCache2.put(xzToLong(X, Z), arrays2);
	}

}
