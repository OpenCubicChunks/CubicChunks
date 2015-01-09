/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package cuchaz.cubicChunks.generator.biome.alternateGen;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import net.minecraft.world.World;

class BiomeFinder
{
	private static final int DIST_DIMENSIONS = 4;

	static final int IGNORE_HEIGHT = 1;
	static final int IGNORE_VOL = 2;
	static final int IGNORE_TEMP = 4;
	static final int IGNORE_RAINFALL = 8;
	static final int HEIGHT_INV = 16;
	static final int VOLATILITY_INV = 32;
	static final int TEMP_INV = 64;
	static final int RAINFALL_INV = 128;
	static final int FORCE_NO_EXTENDED_HEIGHT_VOL_CHEKCS = 256;
	static final int NO_RARITY = 512;

	private final IBiomeDistCalc heightDistCalc;
	private final IBiomeDistCalc volDistCalc;
	private final IBiomeDistCalc tempDistCalc;
	private final IBiomeDistCalc rainfallDistCalc;

	private final boolean ignoreTemp;
	private final boolean ignoreRainfall;
	private final boolean ignoreVol;
	private final boolean ignoreHeight;
	private final boolean noExtHV;
	private final boolean noRarity;

	private final AlternateBiomeGen biomeGen;

	private static boolean checkRange( double min, double max, double value, boolean ignore )
	{
		return ignore || (value >= min && value <= max);
	}

	private static double distSquared( double... xn )
	{
		double d = 0;
		for( double x: xn )
		{
			d += x * x;
		}
		return d;
	}

	BiomeFinder( World world, AlternateBiomeGen biomeGen, int flags )
	{
		this.heightDistCalc = (flags & HEIGHT_INV) == 0 ? new NormalDistCalc() : new InvDistCalc();
		this.volDistCalc = (flags & VOLATILITY_INV) == 0 ? new NormalDistCalc() : new InvDistCalc();
		this.tempDistCalc = (flags & TEMP_INV) == 0 ? new NormalDistCalc() : new InvDistCalc();
		this.rainfallDistCalc = (flags & RAINFALL_INV) == 0 ? new NormalDistCalc() : new InvDistCalc();
		this.ignoreHeight = (flags & IGNORE_HEIGHT) != 0;
		this.ignoreVol = (flags & IGNORE_VOL) != 0;
		this.ignoreTemp = (flags & IGNORE_TEMP) != 0;
		this.ignoreRainfall = (flags & IGNORE_RAINFALL) != 0;
		this.noExtHV = (flags & FORCE_NO_EXTENDED_HEIGHT_VOL_CHEKCS) != 0;
		this.noRarity = (flags & NO_RARITY) != 0;

		this.biomeGen = biomeGen;
	}

	CubeBiomeGenBase getBiomeForValues( double x, double z, double vol, double height, double temp, double rainfall )
	{
		double minDistSquaredInRange = Double.MAX_VALUE;

		CubeBiomeGenBase nearestBiomeInRange = null;

		int biomeNum = 0;//used only for generating rarity. Biome order shouldn't change.

		//iterate over all biomes and find the "nearest" biome
		for( AlternateBiomeGenInfo biome: AlternateBiomeGen.BIOMES )
		{
			boolean extHV = !noExtHV & biome.extendedHeightVolatilityChecks;

			//max volatility for this biome. If we include volatility in height checks volatility can naver be higher that avgHeight - minHeight ( = 0.5*(maxHeight-minHeight) )
			double maxVol = extHV ? Math.min( (biome.maxHeight - biome.minHeight) * 0.5, biome.maxVolatility ) : biome.maxVolatility;

			//check if volatility and height are in correct range
			boolean heightOK;
			if( extHV )
			{
				//min and max height if we include volatility in height checks
				double heightWithVolatilityMin = height - vol;
				double heightWithVolatilityMax = height + vol;

				//are we between min and max height (including volatility)?
				heightOK = checkRange( biome.minHeight, biome.maxHeight, heightWithVolatilityMin, ignoreHeight )
					&& checkRange( biome.minHeight, biome.maxHeight, heightWithVolatilityMax, ignoreHeight );
			}
			else
			{
				heightOK = checkRange( biome.minHeight, biome.maxHeight, height, ignoreHeight );
			}
			boolean volatilityOK = checkRange( biome.minVolatility, maxVol, vol, ignoreVol );
			boolean tempOK = checkRange( biome.minTemp, biome.maxTemp, temp, ignoreTemp );
			boolean rainfallOK = checkRange( biome.minRainfall, biome.maxRainfall, rainfall, ignoreRainfall );

			//calculate distances
			double heightDist = heightDistCalc.dist( biome.minHeight, biome.maxHeight, height );
			double volDist = volDistCalc.dist( biome.minVolatility, biome.maxVolatility, vol );
			double rainfallDist = rainfallDistCalc.dist( biome.minRainfall, biome.maxRainfall, rainfall );
			double tempDist = tempDistCalc.dist( biome.minTemp, biome.maxTemp, temp );

			//calculate distSquared
			double distSquared = distSquared( heightDist, volDist, rainfallDist, tempDist );

			if( !noRarity )
			{
				//noise values are from -1 to 1. 
				//Corner cases (impossible):
				//	n	r	(n = noise, r = rarity, d = distSquared)
				//----------------------------------------------------
				//	-1	-1	--> rarityModifier = -DIST_DIMENSIONS --> distSquared += DIST_DIMENSIONS; (far biome)
				//	0	-1	--> rarityModifier = -DIST_DIMENSIONS/2 --> distSquared += DIST_DIMENSIONS/2; (far biome)
				//	1	-1	--> rarityModifier = 0 --> distSquared stays the same
				//----------------------------------------------------
				//	-1	1	--> rarityModifier = 0 --> distSquared stays the same
				//	0	1	-->	rarityModifier = DIST_DIMENSIONS/2 --> distSquared -= DIST_DIMENSIONS/2; (near biome)
				//	1	1	--> rarityModifier = DIST_DIMENSIONS --> distSquared -= DIST_DIMENSIONS; (near biome)

				double rarityModifier = biomeGen.getRarityMod( biomeNum, x, z );
				rarityModifier *= DIST_DIMENSIONS / 2.0001D;

				assert rarityModifier > -DIST_DIMENSIONS && rarityModifier < DIST_DIMENSIONS : String.format("Wrong rarity value: %.5f, x: %.2f, z: %.2f ",biomeGen.getRarityMod( biomeNum, x, z ),x,z);

				distSquared += rarityModifier;
			}

			//Are we in correct value range for the biome? Is it the nearest biome?
			if( heightOK && volatilityOK && tempOK && rainfallOK && distSquared < minDistSquaredInRange )
			{
				nearestBiomeInRange = biome.biome;
				minDistSquaredInRange = distSquared;
			}
			biomeNum++;
		}

		return nearestBiomeInRange;
	}

	private interface IBiomeDistCalc
	{
		abstract double dist( double min, double max, double val );
	}

	private class InvDistCalc implements IBiomeDistCalc
	{
		@Override
		public double dist( double min, double max, double val )
		{
			double x = min - max;
			return ((min + max) * 0.5D - val) / x + x * x * 0.5 - 0.5;
		}
	}

	private class NormalDistCalc implements IBiomeDistCalc
	{
		@Override
		public double dist( double min, double max, double val )
		{
			return ((min + max) * 0.5D - val) * (min - max);
		}
	}

}
