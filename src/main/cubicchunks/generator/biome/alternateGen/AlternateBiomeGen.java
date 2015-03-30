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

import static cuchaz.cubicChunks.generator.biome.biomegen.CCBiome.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import libnoiseforjava.SimplexBasis;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class AlternateBiomeGen {
	public static final List<AlternateBiomeGenInfo> BIOMES = new ArrayList<AlternateBiomeGenInfo>(256);

	static {
		init();
	}

	static void init() {
		System.out.println(Thread.currentThread().getId());
		// ocean biomes
		registerBiome(AlternateBiomeGenInfo.builder().setH(-0.5F, -0.03F).setV(0.0F, 0.5F).setT(0.2F, 1.0F)
				.setR(0.0F, 1.0F).setSizeRarity(1.0F, -0.5F).setExtHV(true).setBiome(ocean).setName("Ocean").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(-1.0F, -0.5F).setV(0.0F, 0.5F).setT(0.2F, 1.0F)
				.setR(0.0F, 1.0F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(deepOcean).setName("Deep ocean")
				.build());
		// sea level biomes
		registerBiome(AlternateBiomeGenInfo.builder().setH(-0.1F, 0.03F).setV(0.0F, 0.01F).setT(0.0F, 1.0F)
				.setR(0.0F, 0.7F).setSizeRarity(1.0F, 0.6F).setExtHV(false).setBiome(beach).setName("Beach").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(-0.1F, 0.03F).setV(0.01F, 0.05F).setT(0.0F, 1.0F)
				.setR(0.0F, 0.7F).setSizeRarity(1.0F, 0.6F).setExtHV(false).setBiome(coldBeach).setName("Cold Beach")
				.build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(-0.1F, 0.26000002F).setV(0.0F, 0.2F).setT(0.6F, 0.90000004F)
				.setR(0.6F, 1.0F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(swampland).setName("Swampland")
				.build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(-0.1F, 0.18F).setV(0.0F, 0.3F).setT(0.6F, 0.90000004F)
				.setR(0.5F, 1.0F).setSizeRarity(2.0F, 0.0F).setExtHV(true).setBiome(mushroomIsland)
				.setName("Mushroom Island").build());
		// mostly flat biomes
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.02F, 0.82F).setV(0.0F, 0.3F).setT(0.82F, 1.0F)
				.setR(0.0F, 0.26F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(desert).setName("Desert").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.02F, 0.22F).setV(0.0F, 0.3F).setT(0.64F, 0.9F)
				.setR(0.1F, 0.6F).setSizeRarity(1.0F, -0.5F).setExtHV(true).setBiome(savanna).setName("Savanna")
				.build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.02F, 0.17999999F).setV(0.12F, 0.28F)
				.setT(0.3F, 0.70000005F).setR(0.4F, 0.8F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(plains)
				.setName("Sunflower plains").buildMutated());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.02F, 0.22F).setV(0.0F, 0.24F).setT(0.3F, 0.70000005F)
				.setR(0.4F, 0.8F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(plains).setName("Plains").build());
		// registerBiome(AlternateBiomeGenInfo.builder().setH(0.025F, 0.4F
		// ).setV(0.0F, 0.15F ).setT(0.0F, 0.3F ).setR(0.3F, 0.6F
		// ).setSizeRarity(1.0F,
		// 1.0F).setExtHV(true).setBiome(icePlains).setName("Ice plains").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.02F, 0.46F).setV(0.0F, 0.34F).setT(0.0F, 0.3F)
				.setR(0.3F, 0.6F).setSizeRarity(1.1F, 0.6F).setExtHV(true).setBiome(icePlains)
				.setName("Ice plains spikes").buildMutated());
		// hilly biomes
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.053F, 0.81299996F).setV(0.0F, 0.5F).setT(0.7F, 1.0F)
				.setR(0.7F, 1.0F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(jungle).setName("Jungle").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.093F, 0.773F).setV(0.0F, 0.5F).setT(0.7F, 1.0F)
				.setR(0.397F, 0.777F).setSizeRarity(3.0F, 0.0F).setExtHV(true).setBiome(roofedForest)
				.setName("Roofed forest").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.1F, 0.82000005F).setV(0.0F, 0.5F).setT(0.35F, 0.89F)
				.setR(0.34F, 1.0F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(forest).setName("Forest").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.1F, 0.78000003F).setV(0.29F, 0.78999996F).setT(0.5F, 0.7F)
				.setR(0.4F, 1.0F).setSizeRarity(1.0F, 0.0F).setExtHV(true).setBiome(forest).setName("Flower forest")
				.buildMutated());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.1F, 0.78000003F).setV(0.0F, 0.5F).setT(0.38F, 0.7F)
				.setR(0.4F, 0.8F).setSizeRarity(1.0F, 1.0F).setExtHV(true).setBiome(birchForest)
				.setName("Birch forest").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.1F, 0.78000003F).setV(0.0F, 0.8F).setT(0.1F, 0.42F)
				.setR(0.0F, 0.92F).setSizeRarity(1.2F, 0.4F).setExtHV(true).setBiome(megaTaiga).setName("Mega taiga")
				.build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.2F, 0.6F).setV(0.0F, 0.5F).setT(0.3F, 0.5F)
				.setR(0.5F, 1.0F).setSizeRarity(1.0F, 0.3F).setExtHV(true).setBiome(taiga).setName("Taiga").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.22F, 0.82000005F).setV(0.0F, 0.5F).setT(0.8F, 1.0F)
				.setR(0.0F, 0.5F).setSizeRarity(1.0F, 0.2F).setExtHV(true).setBiome(mesa).setName("Mesa").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.2F, 0.6F).setV(0.1F, 1.0F).setT(0.8F, 1.0F)
				.setR(0.0F, 0.5F).setSizeRarity(1.1F, 0.2F).setExtHV(true).setBiome(mesa).setName("Mesa Bryce")
				.buildMutated());
		// registerBiome(AlternateBiomeGenInfo.builder().setH(0.2F, 0.6F
		// ).setV(0.0F, 0.5F ).setT(0.0F, 0.16F ).setR(0.0F, 1.0F
		// ).setSizeRarity(1.0F,
		// 0.0F).setExtHV(true).setBiome(coldTaiga).setName("Cold taiga").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.56F, 0.84000003F).setV(0.0F, 0.24F).setT(0.8F, 1.0F)
				.setR(0.0F, 0.5F).setSizeRarity(3.0F, 0.0F).setExtHV(true).setBiome(mesaPlateauF)
				.setName("Mesa plateau F").build());
		// mountainous biomes
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.72F, 1.0F).setV(0.3F, 1.0F).setT(0.5F, 1.0F)
				.setR(0.0F, 1.0F).setSizeRarity(1.0F, -1.0F).setExtHV(true).setBiome(extremeHillsPlus)
				.setName("Extreme hills plus").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.7F, 0.94F).setV(0.06F, 1.0F).setT(0.3F, 0.6F)
				.setR(0.0F, 1.0F).setSizeRarity(1.0F, 1.0F).setExtHV(false).setBiome(extremeHills)
				.setName("Extreme hills").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.66F, 0.70000005F).setV(0.0F, 0.7F).setT(0.3F, 0.6F)
				.setR(0.0F, 1.0F).setSizeRarity(0.1F, 0.9F).setExtHV(false).setBiome(extremeHillsEdge)
				.setName("Extreme hills edge").build());
		registerBiome(AlternateBiomeGenInfo.builder().setH(0.7F, 0.98F).setV(0.06F, 1.0F).setT(0.0F, 0.5F)
				.setR(0.0F, 1.0F).setSizeRarity(0.3F, 0.0F).setExtHV(true).setBiome(iceMountains)
				.setName("Ice mountains").build());
	}

	private static final int RARITY_NOISE_LENGTH = 512;

	private final long seed;
	private final float[][] rarityNoise;

	private final double[] randX;
	private final double[] randZ;

	private final double[] cos;
	private final double[] sin;

	private static void registerBiome(AlternateBiomeGenInfo biome) {
		BIOMES.add(biome);
	}

	public AlternateBiomeGen(World world) {
		this.rarityNoise = new float[RARITY_NOISE_LENGTH][RARITY_NOISE_LENGTH];
		this.seed = world.getSeed();
		SimplexBasis simplex = new SimplexBasis();
		simplex.setSeed((int) ((seed & 0xFFFFFFFF) ^ (seed >>> 32)));
		double amplitude = 1;
		double freq = 1D / 128D;
		int oct = 3;
		final double ampConst = 1 / (2 - Math.pow(0.5, oct - 1));
		amplitude *= ampConst;
		double max = 0;
		double min = 0;
		for (int i = 0; i < oct; i++) {
			// 4D noise math trick to generate seamless noise
			double scale = rarityNoise.length * freq;
			for (int x = 0; x < rarityNoise.length; x++) {
				double alfa = x * 2 * Math.PI / rarityNoise.length;
				double xAlfa = Math.sin(alfa) / (2 * Math.PI);
				double zAlfa = Math.cos(alfa) / (2 * Math.PI);
				for (int z = 0; z < rarityNoise[x].length; z++) {
					double beta = z * 2 * Math.PI / rarityNoise[x].length;

					double xBeta = Math.sin(beta) / (2 * Math.PI);
					double zBeta = Math.cos(beta) / (2 * Math.PI);
					assert rarityNoise.length == rarityNoise[x].length;

					rarityNoise[x][z] += simplex.getValue4D(xAlfa * scale, zAlfa * scale, xBeta * scale, zBeta * scale)
							* amplitude;
					if (rarityNoise[x][z] > max) {
						max = rarityNoise[x][z];
					}
					if (rarityNoise[x][z] < min) {
						min = rarityNoise[x][z];
					}
				}
			}
			amplitude /= 2;
			freq *= 2;
		}
		// scale the noise to that max value is 1 and min value is -1
		for (float[] noise : rarityNoise) {
			for (int z = 0; z < noise.length; z++) {
				if (noise[z] < 0) {
					noise[z] /= max;
				} else {
					noise[z] /= -min;
				}
			}
		}
		Random rand = new Random(seed);

		randX = new double[BIOMES.size()];
		randZ = new double[BIOMES.size()];

		cos = new double[BIOMES.size()];
		sin = new double[BIOMES.size()];

		for (int i = 0; i < BIOMES.size(); i++) {
			randX[i] = rand.nextDouble() * 2 - 1;
			randZ[i] = rand.nextDouble() * 2 - 1;

			double randAngle = rand.nextDouble() * Math.PI * 2;
			cos[i] = Math.cos(randAngle);
			sin[i] = Math.sin(randAngle);
		}
	}

	public double getRarityMod(int biome, double x, double z) {
		// rotate
		double sin = this.sin[biome];
		double cos = this.cos[biome];
		double newX = (x * cos - z * sin);
		double newZ = (x * sin + z * cos);

		// random x/z shift
		newX += randX[biome] * RARITY_NOISE_LENGTH;
		newZ += randZ[biome] * RARITY_NOISE_LENGTH;

		// scale for biome
		double scale = 1.0D / BIOMES.get(biome).size;
		newX *= scale;
		newZ *= scale;

		// loop x/z coords
		// assume that RARITY_NOISE_LENGTH is power of 2
		int intX = MathHelper.floor_double(newX);
		int intZ = MathHelper.floor_double(newZ);

		double xFrac = newX - intX;
		double zFrac = newZ - intZ;

		int max = RARITY_NOISE_LENGTH - 1;
		intX &= max;
		intZ &= max;

		float valX0Z0 = rarityNoise[intX][intZ];
		float valX0Z1 = rarityNoise[intX][(intZ + 1) & max];

		float valX1Z0 = rarityNoise[(intX + 1) & max][intZ];
		float valX1Z1 = rarityNoise[(intX + 1) & max][(intZ + 1) & max];

		double lerp1 = valX0Z0 + zFrac * (valX0Z1 - valX0Z0);
		double lerp2 = valX1Z0 + zFrac * (valX1Z1 - valX1Z0);

		double lerp = lerp1 + xFrac * (lerp2 - lerp1);
		assert (zFrac <= 1.0 && xFrac <= 1.0);
		assert (zFrac >= 0.0 && xFrac >= 0.0);
		if (lerp > 1.0) {
			lerp = 1.0D;
		}
		if (lerp < -1.0) {
			lerp = -1.0D;
		}// round-off error correction
		assert Math.abs(BIOMES.get(biome).rarity) <= 1.0 : String.format("Biome rarity too high %f, %s",
				BIOMES.get(biome).rarity, BIOMES.get(biome).name);
		return BIOMES.get(biome).rarity + lerp;
	}
}
