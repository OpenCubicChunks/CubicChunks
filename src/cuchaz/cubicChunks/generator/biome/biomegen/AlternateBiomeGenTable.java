package cuchaz.cubicChunks.generator.biome.biomegen;

import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.beach;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.desert;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.extremeHills;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.forest;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.ocean;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.plains;

public class AlternateBiomeGenTable
{
	private static int i = 1;
	private static final int UNDEFINED = 0;
	public static final int OCEAN = i++,
		PLAINS = i++,
		DESERT = i++,
		EXTREME_HILLS = i++,
		FOREST = i++,
		TAIGA = i++,
		SWAMPLAND = i++,
		RIVER = UNDEFINED,
		HELL = UNDEFINED,
		SKY = UNDEFINED;

	public static final AlternateWorldGenData[] DATA =
	{
		//!!!DO NOT REFORMAT THIS!!!
		//AlternateWorldGenData(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	hDiff,	biome)
		new AlternateWorldGenData(	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	null ), //UNDEFINED
		new AlternateWorldGenData(	0.0F,	0.5F,	-1.0F,	0.0F,	0.0F,	1.0F,	0.0F,	1.0F,	ocean ), //OCEAN
		new AlternateWorldGenData(	0.0F,	0.15F,	0.0F,	0.5F,	0.5F,	0.9F,	0.4F,	0.8F,	plains ), //PLAINS
		new AlternateWorldGenData(	0.0F,	0.4F,	0.0F,	1.0F,	0.9F,	1.0F,	0.0F,	0.2F,	desert ), //DESERT
		new AlternateWorldGenData(	0.3F,	1.0F,	0.0F,	1.0F,	0.0F,	0.3F,	0.3F,	0.8F,	extremeHills), //EXTREME_HILS
		new AlternateWorldGenData(	0.0F,	0.3F,	0.1F,	0.7F,	0.4F,	0.7F,	0.5F,	0.8F,	forest ), //FOREST
		new AlternateWorldGenData(	0.0F,	0.05F,	-0.05F,	0.05F,	0.0F,	1.0F,	0.0F,	1.0F,	beach),
		/*new AlternateWorldGenData(), //TAIGA
		new AlternateWorldGenData(), //SWAMPLAND
		new AlternateWorldGenData(), 
		new AlternateWorldGenData(), 
		new AlternateWorldGenData(), 
		new AlternateWorldGenData(), 
		new AlternateWorldGenData(), 
		new AlternateWorldGenData(), 
		 new AlternateWorldGenData()
		//...*/
	};

	private AlternateBiomeGenTable()
	{
		throw new UnsupportedOperationException();//can't happen, unless using reflection
	}

	public static class AlternateWorldGenData
	{
		public float minVolatility, maxVolatility;
		public float minHeightWithVol, maxHeightWithVol;
		public float minTemp, maxTemp;
		public float minRainfall, maxRainfall;
		public CubeBiomeGenBase biome;

		public AlternateWorldGenData( float minVol, float maxVol, float minHeight, float maxHeight, float minTemp, float maxTemp, float minRainfall, float maxRainfall, CubeBiomeGenBase biome)
		{
			this.minVolatility = minVol;
			this.maxVolatility = maxVol;
			this.minHeightWithVol = minHeight;
			this.maxHeightWithVol = maxHeight;
			this.minTemp = minTemp;
			this.maxTemp = maxTemp;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
			this.biome = biome;
		}
	}
}
