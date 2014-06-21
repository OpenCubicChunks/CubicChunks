package cuchaz.cubicChunks.generator.biome.biomegen;

import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.beach;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.desert;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.extremeHills;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.forest;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.ocean;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.plains;

public class AlternateBiomeGenTable
{
	public static final AlternateWorldGenData[] BIOMES =
	{
		//!!!!!DO NOT REFORMAT THIS!!!!!
		
//		new AlternateWorldGenData(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	addVol,	biome)
//		new AlternateWorldGenData(	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	false,	null ),//I don't remember why I added this. Removed until I need it again...
		new AlternateWorldGenData(	0.0F,	0.5F,	-1.0F,	0.0F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	ocean ),
		new AlternateWorldGenData(	0.0F,	0.15F,	0.0F,	0.5F,	0.5F,	0.9F,	0.4F,	0.8F,	true,	plains ),
		new AlternateWorldGenData(	0.0F,	0.4F,	0.0F,	1.0F,	0.9F,	1.0F,	0.0F,	0.2F,	true,	desert ),
		new AlternateWorldGenData(	0.3F,	1.0F,	0.0F,	1.0F,	0.0F,	0.3F,	0.3F,	0.8F,	true,	extremeHills),
		new AlternateWorldGenData(	0.0F,	0.3F,	0.1F,	0.7F,	0.4F,	0.7F,	0.5F,	0.8F,	true,	forest ),
		new AlternateWorldGenData(	0.0F,	0.05F,	-0.05F,	0.05F,	0.0F,	1.0F,	0.0F,	1.0F,	false,	beach),
		//more biomes here...
	};

	private AlternateBiomeGenTable()
	{
		throw new UnsupportedOperationException();//can't happen, unless using reflection
	}

	public static class AlternateWorldGenData
	{
		public final float minVolatility, maxVolatility;
		public final float minHeight, maxHeight;
		public final float minTemp, maxTemp;
		public final float minRainfall, maxRainfall;
		public final boolean entendedHeightVolatilityChecks;
		public final CubeBiomeGenBase biome;

		public AlternateWorldGenData( float minVol, float maxVol, float minHeight, float maxHeight, float minTemp, float maxTemp, float minRainfall, float maxRainfall, boolean ignoreVolatilityHeight, CubeBiomeGenBase biome)
		{
			this.minVolatility = minVol;
			this.maxVolatility = maxVol;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
			this.minTemp = minTemp;
			this.maxTemp = maxTemp;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
			this.entendedHeightVolatilityChecks = ignoreVolatilityHeight;
			this.biome = biome;
		}
	}
}
