package cuchaz.cubicChunks.generator.biome.biomegen;

import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.beach;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.desert;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.extremeHills;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.forest;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.ocean;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.plains;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.World;

public class AlternateBiomeGenTable
{
	
	public static final AlternateWorldGenData[] BIOMES =
	{
		//!!!!!DO NOT REFORMAT THIS!!!!!
		
//		new AlternateWorldGenData(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	addVol,	biome)
		//rarity: -1 to 1. -1 = The lowest, 1 = the highest. -0.8 is MUCH more rare than -0.8, but -01 is  only a bit more rare than 0.0
		//size: any positive value. 1 = default. Rare biomes are smaller.
//		new AlternateWorldGenData(	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	-1.0F,	false,	null ),//I don't remember why I added this. Removed until I need it again...
		new AlternateWorldGenData(	0.0F,	0.5F,	-1.0F,	0.0F,	0.0F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	ocean ),
		new AlternateWorldGenData(	0.0F,	0.15F,	0.0F,	0.5F,	0.5F,	0.9F,	0.4F,	0.8F,	0.0F,	1.0F,	true,	plains ),
		new AlternateWorldGenData(	0.0F,	0.4F,	0.0F,	1.0F,	0.9F,	1.0F,	0.0F,	0.2F,	0.0F,	1.0F,	true,	desert ),
		new AlternateWorldGenData(	0.3F,	1.0F,	0.0F,	1.0F,	0.0F,	0.3F,	0.3F,	0.8F,	0.0F,	1.0F,	true,	extremeHills),
		new AlternateWorldGenData(	0.0F,	0.3F,	0.1F,	0.7F,	0.4F,	0.7F,	0.5F,	0.8F,	0.0F,	1.0F,	true,	forest ),
		new AlternateWorldGenData(	0.0F,	0.05F,	-0.05F,	0.05F,	0.0F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	false,	beach),
		//more biomes here...
	};
	private static final AlternateBiomeGenTable instance = new AlternateBiomeGenTable();
	private final Map<String, IBuilder> rarityBuilders = new HashMap<String, IBuilder>( 2 );

	public static IBuilder getRarityGenerator( World world )
	{
		return instance.rarityBuilders.get( ((CubeWorldServer)world).getWorldInfo().getWorldName());
	}

	public static void createBuilderForWorld( World world )
	{
		BasicBuilder builder = new BasicBuilder();
		builder.setSeed( (int)((world.getSeed() & 0xFFFFFFFF) ^ (world.getSeed() >>> 32)) );
		builder.setMaxElev( 1 );
		builder.setOctaves( 2 );
		builder.setFreq( 0.001, 0.001, 0.001 );
		builder.build();
		instance.rarityBuilders.put( ((CubeWorldServer)world).getWorldInfo().getWorldName(), builder);
	}

	private AlternateBiomeGenTable()
	{
		if( instance != null )
		{
			throw new UnsupportedOperationException();//can't happen, unless using reflection
		}
	}

	public static class AlternateWorldGenData
	{
		public final float minVolatility, maxVolatility;
		public final float minHeight, maxHeight;
		public final float minTemp, maxTemp;
		public final float minRainfall, maxRainfall;
		public final boolean entendedHeightVolatilityChecks;
		public final CubeBiomeGenBase biome;
		public final double rarity, size;

		public AlternateWorldGenData( float minVol, float maxVol, float minHeight, float maxHeight, float minTemp, float maxTemp, float minRainfall, float maxRainfall, float rarity, float size, boolean ignoreVolatilityHeight, CubeBiomeGenBase biome )
		{
			this.minVolatility = minVol;
			this.maxVolatility = maxVol;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
			this.minTemp = minTemp;
			this.maxTemp = maxTemp;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
			this.rarity = rarity;
			this.size = size;
			this.entendedHeightVolatilityChecks = ignoreVolatilityHeight;
			this.biome = biome;
		}
	}
}
