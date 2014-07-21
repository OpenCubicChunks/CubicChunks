package cuchaz.cubicChunks.generator.biome.alternateGen;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.*;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import libnoiseforjava.SimplexBasis;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.util.Color;

public class AlternateBiomeGen
{
	/*public static final AlternateBiomeGenInfo[] BIOMES =
	 {
	 //!!!!!DO NOT REFORMAT THIS!!!!!
	 //rarity: -1 to 1. -1 = The lowest, 1 = the highest. -0.8 is MUCH more rare than -0.8, but -01 is  only a bit more rare than 0.0
	 //size: any positive value. 1 = default. Rare biomes are smaller.
		
	 //		new AlternateBiomeGenInfo(	vMin,	vMax,	hMin,	hMax,	tMin,	tMax,	rMin,	rMax,	rarity,	size,	addVol,	biome)//!!!!!DO NOT REFORMAT THIS!!!!!
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
	 };*/
	public static final List<AlternateBiomeGenInfo> BIOMES = new ArrayList<AlternateBiomeGenInfo>( 256 );

	static
	{
		init();
	}

	static void init()
	{
		System.out.println( Thread.currentThread().getId() );
		//sea level biomes
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.5F, -0.03F ).setV( 0.0F, 0.5F ).setT( 0.2F, 1.0F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, -0.5F ).setExtHV( true ).setBiome( ocean ).setName( "Ocean" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -1.0F, -0.5F ).setV( 0.0F, 0.5F ).setT( 0.2F, 1.0F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( deepOcean ).setName( "Deep ocean" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -1.0F, -0.5F ).setV( 0.0F, 0.5F ).setT( 0.0F, 0.2F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( frozenOcean ).setName( "Frozen ocean" ).build() );
		//just above sea level flat biomes
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.05F, 0.05F ).setV( 0.0F, 0.05F ).setT( 0.5F, 1.0F ).setR( 0.0F, 0.7F ).setSizeRarity( 1.0F, 0.6F ).setExtHV( false ).setBiome( beach ).setName( "Beach" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.05F, 0.05F ).setV( 0.0F, 0.1F ).setT( 0.0F, 0.5F ).setR( 0.0F, 0.7F ).setSizeRarity( 1.0F, 0.6F ).setExtHV( false ).setBiome( coldBeach ).setName( "Cold beach" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.05F, 0.05F ).setV( 0.0F, 0.2F ).setT( 0.6F, 0.9F ).setR( 0.7F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( swampland ).setName( "Swampland" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.05F, 0.3F ).setV( 0.05F, 0.3F ).setT( 0.6F, 0.9F ).setR( 0.7F, 1.0F ).setSizeRarity( 1.0F, 0.7F ).setExtHV( false ).setBiome( swampland ).setName( "Swampland M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( -0.1F, 0.4F ).setV( 0.0F, 0.3F ).setT( 0.6F, 0.9F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 1.0F ).setExtHV( true ).setBiome( mushroomIsland ).setName( "Mushroom Island" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.0F, 0.3F ).setT( 0.8F, 1.0F ).setR( 0.0F, 0.3F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( desert ).setName( "Desert" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.3F, 0.5F ).setT( 0.8F, 1.0F ).setR( 0.0F, 0.3F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( desert ).setName( "Desert M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.0F, 0.3F ).setT( 0.6F, 0.8F ).setR( 0.1F, 0.6F ).setSizeRarity( 1.0F, -0.5F ).setExtHV( true ).setBiome( savanna ).setName( "Savanna" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.3F, 0.5F ).setT( 0.6F, 0.8F ).setR( 0.1F, 0.6F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( savanna ).setName( "Savanna M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.15F, 0.3F ).setT( 0.3F, 0.7F ).setR( 0.4F, 0.8F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( plains ).setName( "Sunflower plains (Plains M)" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.0F, 0.15F ).setT( 0.3F, 0.7F ).setR( 0.4F, 0.8F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( plains ).setName( "Plains" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.0F, 0.15F ).setT( 0.0F, 0.3F ).setR( 0.3F, 0.6F ).setSizeRarity( 1.0F, 1.0F ).setExtHV( true ).setBiome( icePlains ).setName( "Ice plains" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.025F, 0.4F ).setV( 0.15F, 0.3F ).setT( 0.0F, 0.3F ).setR( 0.3F, 0.6F ).setSizeRarity( 1.1F, 0.6F ).setExtHV( true ).setBiome( icePlains ).setName( "Ice plains spikes" ).buildMutated() );
		//rolling hills biomes
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.8F, 1.0F ).setR( 0.7F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( jungle ).setName( "Jungle" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.8F, 1.0F ).setR( 0.7F, 1.0F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( jungle ).setName( "Jungle M" ).buildMutated() );
		//new AlternateBiomeGenInfo(	0.0F,	0.5F,	0.2F,	0.6F,	0.75F,	0.8F,	0.75F,	0.8F,	1.0F,	0.1F,	false, 	new Color(40, 255, 20) ),//jungle edge 
		//new AlternateBiomeGenInfo(	0.0F,	0.15F,	0.0F,			0.2F,	0.0F,	0.2F,	0.3F,	0.6F,	1.1F,	CubeBiomeGenBase.getBiome(151) ),//Jungle edge M
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.7F, 1.0F ).setR( 0.5F, 0.7F ).setSizeRarity( 3.0F, 0.0F ).setExtHV( true ).setBiome( roofedForest ).setName( "Roofed forest" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.7F, 1.0F ).setR( 0.5F, 0.7F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( roofedForest ).setName( "Roofed forest M" ).buildMutated() );
		//registerBiome( AlternateBiomeGenInfo.builder().setH( 0.3F, 0.6F ).setV( 0.0F, 0.15F ).setT( 0.6F, 0.8F ).setR( 0.1F, 0.6F ).setSizeRarity( 3.0F, 0.0F ).setExtHV( true ).setBiome( new Color( 100, 80, 0 ) ).setName( "Savanna plateau" ).build() );
		//registerBiome( AlternateBiomeGenInfo.builder().setH( 0.3F, 0.6F ).setV( 0.15F, 0.4F ).setT( 0.6F, 0.8F ).setR( 0.1F, 0.6F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( new Color( 100, 80, 40 ) ).setName( "Savanna plateau M" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.5F, 0.7F ).setR( 0.7F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( forest ).setName( "Forest" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.5F, 0.7F ).setR( 0.4F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( forest ).setName( "Flower forest" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.5F, 0.7F ).setR( 0.4F, 0.7F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( birchForest ).setName( "Birch forest" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.5F, 0.7F ).setR( 0.4F, 0.7F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( birchForest ).setName( "Borch forest M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.8F ).setT( 0.3F, 0.5F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.2F, 0.4F ).setExtHV( true ).setBiome( megaTaiga ).setName( "Mega taiga" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.3F, 0.5F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.3F ).setExtHV( true ).setBiome( taiga ).setName( "Taiga" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.3F, 0.5F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.3F ).setExtHV( true ).setBiome( taiga ).setName( "Taiga M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.3F, 0.6F ).setR( 0.0F, 0.5F ).setSizeRarity( 1.0F, 0.2F ).setExtHV( true ).setBiome( mesa ).setName( "Mesa" ).build() );
		//registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.3F, 0.6F ).setR( 0.0F, 0.5F ).setSizeRarity( 1.1F, 0.2F ).setExtHV( true ).setBiome( new Color( 255, 100, 200 ) ).setName( "Mesa Bryce" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.0F, 0.5F ).setT( 0.0F, 0.3F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( coldTaiga ).setName( "Cold taiga" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.2F, 0.6F ).setV( 0.5F, 1.0F ).setT( 0.0F, 0.3F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( coldTaiga ).setName( "Cold taiga M" ).buildMutated() );
		//large hills biomes
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.8F ).setT( 0.8F, 1.0F ).setR( 0.0F, 0.5F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( desertHills ).setName( "Desert hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.8F ).setT( 0.7F, 1.0F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( jungleHills ).setName( "Jungle hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.15F ).setT( 0.4F, 0.8F ).setR( 0.0F, 0.5F ).setSizeRarity( 3.0F, 0.0F ).setExtHV( true ).setBiome( mesaPlateau ).setName( "Mesa plateau" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.15F, 0.4F ).setT( 0.4F, 0.8F ).setR( 0.0F, 0.5F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( mesaPlateau ).setName( "Mesa plateau M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.15F ).setT( 0.4F, 0.8F ).setR( 0.0F, 0.5F ).setSizeRarity( 3.0F, 0.0F ).setExtHV( true ).setBiome( mesaPlateauF ).setName( "Mesa plateau F" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.15F, 0.4F ).setT( 0.4F, 0.8F ).setR( 0.0F, 0.5F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( mesaPlateauF ).setName( "Mesa plateau F M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.8F ).setT( 0.5F, 0.7F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( forestHills ).setName( "Forest hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.5F ).setT( 0.3F, 0.5F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( birchForestHills ).setName( "Birch forest hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.5F, 1.0F ).setT( 0.3F, 0.5F ).setR( 0.5F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( birchForestHills ).setName( "Birch forest hills M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.5F ).setT( 0.2F, 0.4F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.2F, 0.4F ).setExtHV( true ).setBiome( megaTaigaHills ).setName( "Mega taiga hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.5F, 1.0F ).setT( 0.2F, 0.4F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.2F, 0.4F ).setExtHV( true ).setBiome( megaTaigaHills ).setName( "Mega spruce taiga hills" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.8F ).setT( 0.2F, 0.4F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.2F, 0.0F ).setExtHV( true ).setBiome( taigaHills ).setName( "Taiga hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.6F, 0.8F ).setV( 0.0F, 0.8F ).setT( 0.0F, 0.3F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 0.0F ).setExtHV( true ).setBiome( coldTaigaHills ).setName( "Cold taiga hills" ).build() );
		//mountain biomes
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.7F, 0.96F ).setV( 0.3F, 1.0F ).setT( 0.5F, 1.0F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, -1.0F ).setExtHV( true ).setBiome( extremeHillsPlus ).setName( "Extreme hills plus" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.96F, 1.0F ).setV( 0.3F, 1.0F ).setT( 0.5F, 1.0F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.1F, 0.0F ).setExtHV( true ).setBiome( extremeHillsPlus ).setName( "Extreme hills plus M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.96F, 1.0F ).setV( 0.3F, 1.0F ).setT( 0.3F, 0.6F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.2F, 0.0F ).setExtHV( true ).setBiome( extremeHills ).setName( "Extreme hills M" ).buildMutated() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.7F, 0.96F ).setV( 0.3F, 1.0F ).setT( 0.3F, 0.6F ).setR( 0.0F, 1.0F ).setSizeRarity( 1.0F, 1.0F ).setExtHV( false ).setBiome( extremeHills ).setName( "Extreme hills" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.65F, 0.7F ).setV( 0.3F, 1.0F ).setT( 0.3F, 0.6F ).setR( 0.0F, 1.0F ).setSizeRarity( 0.1F, 0.9F ).setExtHV( false ).setBiome( extremeHillsEdge ).setName( "Extreme hills edge" ).build() );
		registerBiome( AlternateBiomeGenInfo.builder().setH( 0.7F, 1.0F ).setV( 0.4F, 1.0F ).setT( 0.0F, 0.3F ).setR( 0.0F, 1.0F ).setSizeRarity( 0.3F, 0.0F ).setExtHV( true ).setBiome( iceMountains ).setName( "Ice mountains" ).build() );
	}

	private final World world;

	private static final int RARITY_NOISE_LENGTH = 512;

	private final long seed;
	private final float[][] rarityNoise;

	private final double[] randX;
	private final double[] randZ;

	private final double[] cos;
	private final double[] sin;

	private static void registerBiome( AlternateBiomeGenInfo biome )
	{
		BIOMES.add( biome );
	}

	public AlternateBiomeGen( World world )
	{
		this.world = world;
		this.rarityNoise = new float[RARITY_NOISE_LENGTH][RARITY_NOISE_LENGTH];
		this.seed = world.getSeed();
		SimplexBasis simplex = new SimplexBasis();
		simplex.setSeed( (int)((seed & 0xFFFFFFFF) ^ (seed >>> 32)) );
		double amplitude = 1;
		double freq = 1D / 128D;
		int oct = 3;
		final double ampConst = 1 / (2 - Math.pow( 0.5, oct - 1 ));
		amplitude *= ampConst;
		double max = 0;
		double min = 0;
		for( int i = 0; i < oct; i++ )
		{
			//4D noise math trick to generate seamless noise
			double scale = rarityNoise.length * freq;
			for( int x = 0; x < rarityNoise.length; x++ )
			{
				double alfa = x * 2 * Math.PI / rarityNoise.length;
				double xAlfa = Math.sin( alfa ) / (2 * Math.PI);
				double zAlfa = Math.cos( alfa ) / (2 * Math.PI);
				for( int z = 0; z < rarityNoise[x].length; z++ )
				{
					double beta = z * 2 * Math.PI / rarityNoise[x].length;

					double xBeta = Math.sin( beta ) / (2 * Math.PI);
					double zBeta = Math.cos( beta ) / (2 * Math.PI);
					assert rarityNoise.length == rarityNoise[x].length;

					rarityNoise[x][z] += simplex.getValue4D( xAlfa * scale, zAlfa * scale, xBeta * scale, zBeta * scale ) * amplitude;
					if( rarityNoise[x][z] > max )
					{
						max = Math.abs( rarityNoise[x][z] );
					}
					if( rarityNoise[x][z] < min )
					{
						min = Math.abs( rarityNoise[x][z] );
					}
				}
			}
			amplitude /= 2;
			freq *= 2;
		}
		//scale the noise to that max value is 1 and min value is -1
		for( float[] noise: rarityNoise )
		{
			for( int z = 0; z < noise.length; z++ )
			{
				if(noise[z] < 0) {
					noise[z] /= max;
				} else
				{
					noise[z] /= -min;
				}
			}
		}
		Random rand = new Random( seed );

		randX = new double[BIOMES.size()];
		randZ = new double[BIOMES.size()];

		cos = new double[BIOMES.size()];
		sin = new double[BIOMES.size()];

		for( int i = 0; i < BIOMES.size(); i++ )
		{
			randX[i] = rand.nextDouble() * 2 - 1;
			randZ[i] = rand.nextDouble() * 2 - 1;

			double randAngle = rand.nextDouble() * Math.PI * 2;
			cos[i] = Math.cos( randAngle );
			sin[i] = Math.sin( randAngle );
		}
	}

	public double getRarityMod( int biome, double x, double z )
	{
		//rotate
		double sin = this.sin[biome];
		double cos = this.cos[biome];
		double newX = (x * cos - z * sin);
		double newZ = (x * sin + z * cos);

		//random x/z shift
		newX += randX[biome] * RARITY_NOISE_LENGTH;
		newZ += randZ[biome] * RARITY_NOISE_LENGTH;

		//scale for biome
		double scale = 1.0D / BIOMES.get( biome ).size;
		newX *= scale;
		newZ *= scale;

		//loop x/z coords
		//assume that RARITY_NOISE_LENGTH is power of 2
		int intX = MathHelper.floor_double( newX );
		int intZ = MathHelper.floor_double( newZ );

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

		return BIOMES.get( biome ).rarity + lerp;
	}
}
