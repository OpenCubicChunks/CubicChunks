package cuchaz.cubicChunks.generator.biome.alternateGen;


import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
//import cuchaz.cubicChunks.generator.biome.biomegen.AlternateBiomeGen.AlternateBiomeGenInfo;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.Color;

import net.minecraft.world.World;

public class AlternateBiomeGen
{
	public static final AlternateBiomeGenInfo[] BIOMES =
	{
		//!!!!!DO NOT REFORMAT THIS!!!!!
		//rarity: -1 to 1. -1 = The lowest, 1 = the highest. -0.8 is MUCH more rare than -0.8, but -0.1 is  only a bit more rare than 0.0
		//size: any positive value. 1 = default. Rare biomes are smaller.
		
//ocean biomes
//		new AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	addVol,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	-0.5F,	-0.03F,	0.2F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.ocean),//ocean
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	-1.0F,	-0.5F,	0.2F,	1.0F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.deepOcean), //Deep Ocean
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	-1.0F,	-0.5F,	0.0F,	0.2F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.frozenOcean ), //Frozen Ocean
//sea level biomes
		//AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	check,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.05F,	-0.05F,	0.05F,	0.5F,	1.0F,	0.0F,	0.7F,	0.6F,	1.0F,	false,	CubeBiomeGenBase.beach),//beach
		new AlternateBiomeGenInfo(	0.0F,	0.1F,	-0.05F,	0.05F,	0.0F,	0.5F,	0.0F,	0.7F, 	0.6F,	1.0F,	false,	CubeBiomeGenBase.coldBeach),//Cold beach
		new AlternateBiomeGenInfo(	0.0F,	0.2F,	-0.05F,	0.05F,	0.6F,	0.9F,	0.7F,	1.0F,	-1.0F, 	1.0F, 	true, 	CubeBiomeGenBase.swampland),//SWAMPLAND
		new AlternateBiomeGenInfo(	0.2F,	0.5F,	-0.05F,	0.05F,	0.6F,	0.9F,	0.7F,	1.0F,	0.0F, 	1.0F,	true,	CubeBiomeGenBase.getBiome(134) ),//Swampland M
		new AlternateBiomeGenInfo(	0.0F,	0.2F,	-0.05F,	0.05F,	0.6F,	0.9F,	0.5F,	1.0F,	1.0F, 	1.0F, 	true, 	CubeBiomeGenBase.mushroomIsland),//MushroomIsland
//just above sea level flat biomes
		//AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	check,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.3F,	0.025F,	0.4F,	0.8F,	1.0F,	0.0F,	0.3F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.desert ),//desert
		new AlternateBiomeGenInfo(	0.3F,	0.5F,	0.025F,	0.4F,	0.8F,	1.0F,	0.0F,	0.3F, 	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(130) ),//Desert M
		new AlternateBiomeGenInfo(	0.0F,	0.3F,	0.025F,	0.4F,	0.6F,	0.8F,	0.1F,	0.6F,	-0.5F,	1.0F,	true, 	CubeBiomeGenBase.savanna), //savanna
		new AlternateBiomeGenInfo(	0.3F,	0.5F,	0.025F,	0.4F,	0.6F,	0.8F,	0.1F,	0.6F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(163) ),//Savanna M
		new AlternateBiomeGenInfo(	0.15F,	0.3F,	0.025F,	0.4F,	0.3F,	0.7F,	0.4F,	0.8F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.getBiome(129) ),//Sunflower Plains
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.025F,	0.4F,	0.3F,	0.7F,	0.4F,	0.8F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.plains ),//plains
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.025F,	0.4F,	0.0F,	0.3F,	0.3F,	0.6F,	0.6F,	1.0F,	true,	CubeBiomeGenBase.icePlains ), //ice plains
		new AlternateBiomeGenInfo(	0.15F,	0.3F,	0.025F,	0.4F,	0.0F,	0.3F,	0.3F,	0.6F,	0.6F,	1.1F,	true,	CubeBiomeGenBase.getBiome(135) ),//Ice Plain Spikes
//rolling hills biomes
		//AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	check,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.8F,	1.0F,	0.7F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.jungle), //jungle 
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.8F,	1.0F,	0.7F,	1.0F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(149) ),//Jungle M
		//new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.75F,	0.8F,	0.75F,	0.8F,	1.0F,	0.1F,	false, 	new Color(40, 255, 20) ),//jungle edge 
		//new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.0F,			0.2F,	0.0F,	0.2F,	0.3F,	0.6F,	1.1F,	CubeBiomeGenBase.getBiome(151) ),//Jungle edge M
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.7F,	1.0F,	0.5F,	0.7F,	0.0F,	3.0F,	true,	CubeBiomeGenBase.roofedForest), //Roofed Forest
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.7F,	1.0F,	0.5F,	0.7F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(157) ),//Roofed Forest M
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.3F,	0.6F,	0.6F,	0.8F,	0.1F,	0.6F,	0.0F,	3.0F,	true, 	CubeBiomeGenBase.SavannaPlateau), //svanna plateau
		new AlternateBiomeGenInfo(	0.15F,	0.4F,	0.3F,	0.6F,	0.6F,	0.8F,	0.1F,	0.6F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(164) ),//Savanna Plateau M
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.5F,	0.7F,	0.7F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.forest ),//forest
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.5F,	0.7F,	0.4F,	1.0F,	0.0F,	1.0F, 	true,	CubeBiomeGenBase.getBiome(132) ),//Flower Forest
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.5F,	0.7F,	0.4F,	0.7F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.birchForest ), //Birch Forest
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.5F,	0.7F,	0.4F,	0.7F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(155) ),//Birch Forest M
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.2F,	0.6F,	0.3F,	0.5F,	0.0F,	1.0F, 	0.4F,	1.2F,	true,	CubeBiomeGenBase.megaTaiga), //Mega Taiga
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.3F,	0.5F,	0.5F,	1.0F, 	0.3F,	1.0F,	true, 	CubeBiomeGenBase.taiga), //TAIGA
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.3F,	0.5F,	0.5F,	1.0F,	0.3F, 	1.0F,	true,	CubeBiomeGenBase.getBiome(133) ),//TAIGA M
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.3F,	0.6F,	0.0F,	0.5F,	0.2F,	1.0F,	true,	CubeBiomeGenBase.mesa ), //mesa
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.3F,	0.6F,	0.0F,	0.5F,	0.2F,	1.1F,	true,	CubeBiomeGenBase.getBiome(165) ),//Mesa Bryce
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.0F,	0.3F,	0.0F,	1.0F, 	0.0F,	1.0F,	true,	CubeBiomeGenBase.coldTaiga), //COld TAIGA
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.2F,	0.6F,	0.0F,	0.3F,	0.0F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.getBiome(158) ),//Cold Taiga M
//large hills biomes
		//AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	check,	biome)
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.6F,	0.8F,	0.8F,	1.0F,	0.0F,	0.5F, 	0.0F,	1.0F,	true,	CubeBiomeGenBase.desertHills ),//desert hills 
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.6F,	0.8F,	0.7F,	1.0F,	0.5F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.jungleHills ),//jungle hills 
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.6F,	0.8F,	0.4F,	0.8F,	0.0F,	0.5F,	0.0F,	3.0F,	true, 	CubeBiomeGenBase.mesaPlateau), //mesa plateau
		new AlternateBiomeGenInfo(	0.15F,	0.4F,	0.6F,	0.8F,	0.4F,	0.8F,	0.0F,	0.5F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(167) ),//Mesa Plateau M
		new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.6F,	0.8F,	0.4F,	0.8F,	0.0F,	0.5F,	0.0F,	3.0F,	true,	CubeBiomeGenBase.mesaPlateauF), //mesa plateau F
		new AlternateBiomeGenInfo(	0.15F,	0.4F,	0.6F,	0.8F,	0.4F,	0.8F,	0.0F,	0.5F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(166) ),//Mesa Plateau F M
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.6F,	0.8F,	0.5F,	0.7F,	0.5F,	1.0F, 	0.0F,	1.0F,	true,	CubeBiomeGenBase.forestHills ), //ForestHills
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.6F,	0.8F,	0.3F,	0.5F,	0.5F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.birchForestHills ), //Birch Hills
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.6F,	0.8F,	0.3F,	0.5F,	0.5F,	1.0F,	0.0F,	1.0F,	true,	CubeBiomeGenBase.getBiome(156) ),//Birch Forest Hills M
		new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.6F,	0.8F,	0.2F,	0.4F,	0.0F,	1.0F, 	0.4F,	1.2F,	true,	CubeBiomeGenBase.megaTaigaHills), //Mega taiga hills
		new AlternateBiomeGenInfo(	0.5F,	1.0F,	0.6F,	0.8F,	0.2F,	0.4F,	0.0F,	1.0F,	0.4F,	1.2F,	true,	CubeBiomeGenBase.getBiome(161) ),//Mega Sprice Taiga Hills
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.6F,	0.8F,	0.2F,	0.4F,	0.0F,	1.0F, 	0.0F,	1.2F,	true,	CubeBiomeGenBase.taigaHills), //taiga hills
		new AlternateBiomeGenInfo(	0.0F,	0.8F,	0.6F,	0.8F,	0.0F,	0.3F,	0.0F,	1.0F, 	0.0F,	1.0F,	true, 	CubeBiomeGenBase.coldTaigaHills), // Cold Taiga hills
//mountain biomes
		//AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	check,	biome)
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.7F,	0.96F,	0.5F,	1.0F,	0.0F,	1.0F, 	-1.0F,	1.0F,	true, 	CubeBiomeGenBase.extremeHillsPlus),//extreme hills +
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.96F,	1.0F,	0.5F,	1.0F,	0.0F,	1.0F,	0.0F,	1.1F,	true,	CubeBiomeGenBase.getBiome(162) ),//ExtremeHills+ M
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.96F,	1.0F,	0.3F,	0.6F,	0.0F,	1.0F, 	0.0F,	1.2F,	true,	CubeBiomeGenBase.getBiome(131) ),//Extreme Hills M
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.7F,	0.96F,	0.3F,	0.6F,	0.0F,	1.0F,	1.0F,	1.0F,	false,	CubeBiomeGenBase.extremeHills),//eh
		new AlternateBiomeGenInfo(	0.3F,	1.0F,	0.65F,	0.7F,	0.3F,	0.6F,	0.0F,	1.0F,	0.9F,	0.1F,	false,	CubeBiomeGenBase.extremeHillsEdge), //extremehills edge
		new AlternateBiomeGenInfo(	0.4F,	1.0F,	0.7F,	1.0F,	0.0F,	0.3F,	0.0F,	1.0F,	0.0F,	0.3F,	true,	CubeBiomeGenBase.iceMountains ), //ice mountain
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
