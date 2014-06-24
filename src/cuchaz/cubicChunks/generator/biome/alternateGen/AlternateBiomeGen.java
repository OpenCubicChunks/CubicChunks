package cuchaz.cubicChunks.generator.biome.alternateGen;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
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

public class AlternateBiomeGen
{
	public static final AlternateBiomeGenInfo[] BIOMES =
	{
		//!!!!!DO NOT REFORMAT THIS!!!!!
		//rarity: -1 to 1. -1 = The lowest, 1 = the highest. -0.8 is MUCH more rare than -0.8, but -01 is  only a bit more rare than 0.0
		//size: any positive value. 1 = default. Rare biomes are smaller.
		
//		new AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	addVol,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	-1.0F,	-0.03F,	0.0F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	ocean ),
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.025F,	0.5F,	0.5F,	0.9F,	0.4F,	0.8F,	0.0F,	1.0F,	true,	plains ),
		new AlternateBiomeGenInfo(	0.0F,	0.4F,	0.025F,	1.0F,	0.9F,	1.0F,	0.0F,	0.2F,	0.0F,	1.0F,	true,	desert ),
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.025F,	1.0F,	0.0F,	0.3F,	0.3F,	0.8F,	0.0F,	1.0F,	true,	extremeHills),
		new AlternateBiomeGenInfo(	0.0F,	0.3F,	0.1F,	0.7F,	0.4F,	0.7F,	0.5F,	0.8F,	0.0F,	1.0F,	true,	forest ),
		new AlternateBiomeGenInfo(	0.0F,	0.05F,	-0.05F,	0.05F,	0.0F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	false,	beach),
		//more biomes here...
	};
	private static final AlternateBiomeGen instance = new AlternateBiomeGen();
	private final Map<String, IBuilder> rarityBuilders = new HashMap<String, IBuilder>( 2 );

	public static IBuilder getRarityGenerator( World world )
	{
		return instance.rarityBuilders.get( world.getWorldInfo().getWorldName());
	}

	public static void createBuilderForWorld( World world )
	{
		BasicBuilder builder = new BasicBuilder();
		builder.setSeed( (int)((world.getSeed() & 0xFFFFFFFF) ^ (world.getSeed() >>> 32)) );//set builder seed using all bits of world seed
		builder.setMaxElev( 1 );
		builder.setOctaves( 1 );
		builder.setFreq( 0.001, Math.E, 0.001 );
		builder.build();
		instance.rarityBuilders.put( world.getWorldInfo().getWorldName(), builder);
	}

	private AlternateBiomeGen()
	{
		if( instance != null )
		{
			throw new UnsupportedOperationException();//can't happen, unless using reflection
		}
	}

	public static class AlternateBiomeGenInfo
	{
		public final float minVolatility, maxVolatility;
		public final float minHeight, maxHeight;
		public final float minTemp, maxTemp;
		public final float minRainfall, maxRainfall;
		public final boolean entendedHeightVolatilityChecks;
		public final CubeBiomeGenBase biome;
		public final double rarity, size;

		public AlternateBiomeGenInfo( float minVol, float maxVol, float minHeight, float maxHeight, float minTemp, float maxTemp, float minRainfall, float maxRainfall, float rarity, float size, boolean ignoreVolatilityHeight, CubeBiomeGenBase biome )
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
