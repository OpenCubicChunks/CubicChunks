/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.generator.biome.biomegen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenBigTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenDoublePlantCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenSwampCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTallGrassCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTreesCube;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;

import cuchaz.cubicChunks.world.Cube;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.world.biome.BiomeGenBase;

public abstract class CubeBiomeGenBase extends net.minecraft.world.biome.BiomeGenBase
{
	private static final Logger logger = LogManager.getLogger();

	/** An array of all the biomes, indexed by biome id. */
	private static final BiomeGenBase[] biomeList;

	static
	{
		//clear existing biome set.
		field_150597_n.clear();
		//set our biome array to BiomeGenBase biome array and clear it.
		BiomeGenBase[] temp = new CubeBiomeGenBase[256];
		try
		{
			Field biomes = BiomeGenBase.class.getDeclaredField( "biomeList" );
			biomes.setAccessible( true );
			temp = (BiomeGenBase[])biomes.get( null );
			for( int i = 0; i < temp.length; i++ )
			{
				temp[i] = null;
			}

		}
		catch( NoSuchFieldException ex )
		{
			logger.fatal( "Impossible exception!", ex );
		}
		catch( SecurityException ex )
		{
			logger.fatal( "SecurityException while initializing CubeBiomeGenBase!", ex );
		}
		catch( IllegalArgumentException ex )
		{
			logger.fatal( "Impossible exception!", ex );
		}
		catch( IllegalAccessException ex )
		{
			logger.fatal( "Impossible exception!", ex );
		}

		biomeList = temp;
	}

	protected static final CubeBiomeGenBase.Height defaultBiomeRange = new CubeBiomeGenBase.Height( 0.1F, 0.2F );
	protected static final CubeBiomeGenBase.Height riverRange = new CubeBiomeGenBase.Height( -0.5F, 0.0F );
	protected static final CubeBiomeGenBase.Height oceanRange = new CubeBiomeGenBase.Height( -1.0F, 0.1F );
	protected static final CubeBiomeGenBase.Height deepOceanRange = new CubeBiomeGenBase.Height( -1.8F, 0.1F );
	protected static final CubeBiomeGenBase.Height PlainsRange = new CubeBiomeGenBase.Height( 0.125F, 0.05F );
	protected static final CubeBiomeGenBase.Height taigaRange = new CubeBiomeGenBase.Height( 0.2F, 0.2F );
	protected static final CubeBiomeGenBase.Height hillsRange = new CubeBiomeGenBase.Height( 0.45F, 0.3F );
	protected static final CubeBiomeGenBase.Height plateauRange = new CubeBiomeGenBase.Height( 1.5F, 0.025F );
	protected static final CubeBiomeGenBase.Height extremeHillsRange = new CubeBiomeGenBase.Height( 1.0F, 0.5F );
	protected static final CubeBiomeGenBase.Height beachRange = new CubeBiomeGenBase.Height( 0.0F, 0.025F );
	protected static final CubeBiomeGenBase.Height stoneBeachRange = new CubeBiomeGenBase.Height( 0.1F, 0.8F );
	protected static final CubeBiomeGenBase.Height mushroomIslandRange = new CubeBiomeGenBase.Height( 0.2F, 0.3F );
	protected static final CubeBiomeGenBase.Height swampRange = new CubeBiomeGenBase.Height( -0.2F, 0.1F );

	private static AlternateWorldGenData oceanData = new AlternateWorldGenData();
	private static AlternateWorldGenData desertData = new AlternateWorldGenData();
	private static AlternateWorldGenData forestData = new AlternateWorldGenData();
	private static AlternateWorldGenData ehData = new AlternateWorldGenData();
	private static AlternateWorldGenData plainsData = new AlternateWorldGenData();
	static{
		oceanData.maxHeight = 0.05F;
		oceanData.minHeight = -1;
		oceanData.minVolatility = 0;
		oceanData.maxVolatility = 0.5F;//allow underwater mountains
		oceanData.maxTemp = 1;
		oceanData.minTemp = 0;
		oceanData.minRainfall = 0;
		oceanData.maxRainfall = 1;
		oceanData.maxHeightDiff = 0.1F;
		
		desertData.maxHeight = 1;
		desertData.minHeight = 0.2F;
		desertData.minVolatility = 0;
		desertData.maxVolatility = 1;
		desertData.maxTemp = 1;
		desertData.minTemp = 0.8F;
		desertData.minRainfall = 0;
		desertData.maxRainfall = 0.2F;
		desertData.maxHeightDiff = 0.2F;
		
		forestData.maxHeight = 0.7F;
		forestData.minHeight = 0.2F;
		forestData.minVolatility = 0;
		forestData.maxVolatility = 0.3F;
		forestData.maxTemp = 0.7F;
		forestData.minTemp = 0.3F;
		forestData.minRainfall = 0.5F;
		forestData.maxRainfall = 1;
		forestData.maxHeightDiff = 0.2F;
		
		ehData.maxHeight = 1;
		ehData.minHeight = 0.3F;
		ehData.minVolatility = 0.1F;
		ehData.maxVolatility = 1;
		ehData.maxTemp = 1;
		ehData.minTemp = 0;
		ehData.minRainfall = 0;
		ehData.maxRainfall = 1;
		ehData.maxHeightDiff = 0.3F;
		
		plainsData.maxHeight = 0.5F;
		plainsData.minHeight = 0.1F;
		plainsData.minVolatility = 0;
		plainsData.maxVolatility = 0.1F;
		plainsData.maxTemp = 1;
		plainsData.minTemp = 0;
		plainsData.minRainfall = 0;
		plainsData.maxRainfall = 1;
		plainsData.maxHeightDiff = 0.1F;
	}
	public static final CubeBiomeGenBase ocean = (new BiomeGenOcean( 0 )).setColor( 112 ).setBiomeName( "Ocean" ).setHeightRange( oceanRange ).setAlternateData( oceanData);
	public static final CubeBiomeGenBase plains = (new BiomeGenPlains( 1 )).setColor( 9286496 ).setBiomeName( "Plains" ).setAlternateData( plainsData);
	public static final CubeBiomeGenBase desert = (new BiomeGenDesert( 2 )).setColor( 16421912 ).setBiomeName( "Desert" ).setDisableRain().setTemperatureAndRainfall( 2.0F, 0.0F ).setHeightRange( PlainsRange ).setAlternateData( desertData);
	public static final CubeBiomeGenBase extremeHills = (new BiomeGenHills( 3, false )).setColor( 6316128 ).setBiomeName( "Extreme Hills" ).setHeightRange( extremeHillsRange ).setTemperatureAndRainfall( 0.2F, 0.3F ).setAlternateData( ehData );
	public static final CubeBiomeGenBase forest = (new BiomeGenForest( 4, 0 )).setColor( 353825 ).setBiomeName( "Forest" ).setAlternateData( forestData);
	public static final CubeBiomeGenBase taiga = (new BiomeGenTaiga( 5, 0 )).setColor( 747097 ).setBiomeName( "Taiga" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.25F, 0.8F ).setHeightRange( taigaRange );
	public static final CubeBiomeGenBase swampland = (new BiomeGenSwamp( 6 )).setColor( 522674 ).setBiomeName( "Swampland" ).func_76733_a( 9154376 ).setHeightRange( swampRange ).setTemperatureAndRainfall( 0.8F, 0.9F );
	public static final CubeBiomeGenBase river = (new BiomeGenRiver( 7 )).setColor( 255 ).setBiomeName( "River" ).setHeightRange( riverRange );
	public static final CubeBiomeGenBase hell = (new BiomeGenHell( 8 )).setColor( 16711680 ).setBiomeName( "Hell" ).setDisableRain().setTemperatureAndRainfall( 2.0F, 0.0F );

	/** Is the biome used for sky world. */
	public static final CubeBiomeGenBase sky = (new BiomeGenEnd( 9 )).setColor( 8421631 ).setBiomeName( "Sky" ).setDisableRain();

	public static final CubeBiomeGenBase frozenOcean = (new BiomeGenOcean( 10 )).setColor( 9474208 ).setBiomeName( "FrozenOcean" ).setEnableSnow().setHeightRange( oceanRange ).setTemperatureAndRainfall( 0.0F, 0.5F );
	public static final CubeBiomeGenBase frozenRiver = (new BiomeGenRiver( 11 )).setColor( 10526975 ).setBiomeName( "FrozenRiver" ).setEnableSnow().setHeightRange( riverRange ).setTemperatureAndRainfall( 0.0F, 0.5F );
	public static final CubeBiomeGenBase icePlains = (new BiomeGenSnow( 12, false )).setColor( 16777215 ).setBiomeName( "Ice Plains" ).setEnableSnow().setTemperatureAndRainfall( 0.0F, 0.5F ).setHeightRange( PlainsRange );
	public static final CubeBiomeGenBase iceMountains = (new BiomeGenSnow( 13, false )).setColor( 10526880 ).setBiomeName( "Ice Mountains" ).setEnableSnow().setHeightRange( hillsRange ).setTemperatureAndRainfall( 0.0F, 0.5F );
	public static final CubeBiomeGenBase mushroomIsland = (new BiomeGenMushroomIsland( 14 )).setColor( 16711935 ).setBiomeName( "MushroomIsland" ).setTemperatureAndRainfall( 0.9F, 1.0F ).setHeightRange( mushroomIslandRange );
	public static final CubeBiomeGenBase mushroomIslandShore = (new BiomeGenMushroomIsland( 15 )).setColor( 10486015 ).setBiomeName( "MushroomIslandShore" ).setTemperatureAndRainfall( 0.9F, 1.0F ).setHeightRange( beachRange );

	/** Beach biome. */
	public static final CubeBiomeGenBase beach = (new BiomeGenBeach( 16 )).setColor( 16440917 ).setBiomeName( "Beach" ).setTemperatureAndRainfall( 0.8F, 0.4F ).setHeightRange( beachRange );

	/** Desert Hills biome. */
	public static final CubeBiomeGenBase desertHills = (new BiomeGenDesert( 17 )).setColor( 13786898 ).setBiomeName( "DesertHills" ).setDisableRain().setTemperatureAndRainfall( 2.0F, 0.0F ).setHeightRange( hillsRange );

	/** Forest Hills biome. */
	public static final CubeBiomeGenBase forestHills = (new BiomeGenForest( 18, 0 )).setColor( 2250012 ).setBiomeName( "ForestHills" ).setHeightRange( hillsRange );

	/** Taiga Hills biome. */
	public static final CubeBiomeGenBase taigaHills = (new BiomeGenTaiga( 19, 0 )).setColor( 1456435 ).setBiomeName( "TaigaHills" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.25F, 0.8F ).setHeightRange( hillsRange );

	/** Extreme Hills Edge biome. */
	public static final CubeBiomeGenBase extremeHillsEdge = (new BiomeGenHills( 20, true )).setColor( 7501978 ).setBiomeName( "Extreme Hills Edge" ).setHeightRange( extremeHillsRange.func_150775_a() ).setTemperatureAndRainfall( 0.2F, 0.3F );

	/** Jungle biome identifier */
	public static final CubeBiomeGenBase jungle = (new BiomeGenJungle( 21, false )).setColor( 5470985 ).setBiomeName( "Jungle" ).func_76733_a( 5470985 ).setTemperatureAndRainfall( 0.95F, 0.9F );
	public static final CubeBiomeGenBase jungleHills = (new BiomeGenJungle( 22, false )).setColor( 2900485 ).setBiomeName( "JungleHills" ).func_76733_a( 5470985 ).setTemperatureAndRainfall( 0.95F, 0.9F ).setHeightRange( hillsRange );
	public static final CubeBiomeGenBase jungleEdge = (new BiomeGenJungle( 23, true )).setColor( 6458135 ).setBiomeName( "JungleEdge" ).func_76733_a( 5470985 ).setTemperatureAndRainfall( 0.95F, 0.8F );
	public static final CubeBiomeGenBase deepOcean = (new BiomeGenOcean( 24 )).setColor( 48 ).setBiomeName( "Deep Ocean" ).setHeightRange( deepOceanRange );
	public static final CubeBiomeGenBase stoneBeach = (new BiomeGenStoneBeach( 25 )).setColor( 10658436 ).setBiomeName( "Stone Beach" ).setTemperatureAndRainfall( 0.2F, 0.3F ).setHeightRange( stoneBeachRange );
	public static final CubeBiomeGenBase coldBeach = (new BiomeGenBeach( 26 )).setColor( 16445632 ).setBiomeName( "Cold Beach" ).setTemperatureAndRainfall( 0.05F, 0.3F ).setHeightRange( beachRange ).setEnableSnow();
	public static final CubeBiomeGenBase birchForest = (new BiomeGenForest( 27, 2 )).setBiomeName( "Birch Forest" ).setColor( 3175492 );
	public static final CubeBiomeGenBase birchForestHills = (new BiomeGenForest( 28, 2 )).setBiomeName( "Birch Forest Hills" ).setColor( 2055986 ).setHeightRange( hillsRange );
	public static final CubeBiomeGenBase roofedForest = (new BiomeGenForest( 29, 3 )).setColor( 4215066 ).setBiomeName( "Roofed Forest" );
	public static final CubeBiomeGenBase coldTaiga = (new BiomeGenTaiga( 30, 0 )).setColor( 3233098 ).setBiomeName( "Cold Taiga" ).func_76733_a( 5159473 ).setEnableSnow().setTemperatureAndRainfall( -0.5F, 0.4F ).setHeightRange( taigaRange ).func_150563_c( 16777215 );
	public static final CubeBiomeGenBase coldTaigaHills = (new BiomeGenTaiga( 31, 0 )).setColor( 2375478 ).setBiomeName( "Cold Taiga Hills" ).func_76733_a( 5159473 ).setEnableSnow().setTemperatureAndRainfall( -0.5F, 0.4F ).setHeightRange( hillsRange ).func_150563_c( 16777215 );
	public static final CubeBiomeGenBase megaTaiga = (new BiomeGenTaiga( 32, 1 )).setColor( 5858897 ).setBiomeName( "Mega Taiga" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.3F, 0.8F ).setHeightRange( taigaRange );
	public static final CubeBiomeGenBase megaTaigaHills = (new BiomeGenTaiga( 33, 1 )).setColor( 4542270 ).setBiomeName( "Mega Taiga Hills" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.3F, 0.8F ).setHeightRange( hillsRange );
	public static final CubeBiomeGenBase extremeHillsPlus = (new BiomeGenHills( 34, true )).setColor( 5271632 ).setBiomeName( "Extreme Hills+" ).setHeightRange( extremeHillsRange ).setTemperatureAndRainfall( 0.2F, 0.3F );
	public static final CubeBiomeGenBase savanna = (new BiomeGenSavanna( 35 )).setColor( 12431967 ).setBiomeName( "Savanna" ).setTemperatureAndRainfall( 1.2F, 0.0F ).setDisableRain().setHeightRange( PlainsRange );
	public static final CubeBiomeGenBase SavannaPlateau = (new BiomeGenSavanna( 36 )).setColor( 10984804 ).setBiomeName( "Savanna Plateau" ).setTemperatureAndRainfall( 1.0F, 0.0F ).setDisableRain().setHeightRange( plateauRange );
	public static final CubeBiomeGenBase mesa = (new BiomeGenMesa( 37, false, false )).setColor( 14238997 ).setBiomeName( "Mesa" );
	public static final CubeBiomeGenBase mesaPlateauF = (new BiomeGenMesa( 38, false, true )).setColor( 11573093 ).setBiomeName( "Mesa Plateau F" ).setHeightRange( plateauRange );
	public static final CubeBiomeGenBase mesaPlateau = (new BiomeGenMesa( 39, false, false )).setColor( 13274213 ).setBiomeName( "Mesa Plateau" ).setHeightRange( plateauRange );

	protected static final WorldGenDoublePlantCube worldGenDoublePlant;

	/** The tree generator. */
	protected WorldGenTreesCube worldGeneratorTrees;

	/** The big tree generator. */
	protected WorldGenBigTreeCube worldGeneratorBigTree;

	/** The swamp tree generator. */
	protected WorldGenSwampCube worldGeneratorSwamp;

	/** The average height of this biome. Default 0.1. */
	public float biomeHeight;

	/** The average volatility of this biome. Default 0.3. */
	public float biomeVolatility;

	private AlternateWorldGenData data;

	protected CubeBiomeGenBase( int biomeID )
	{
		super( biomeID );
		this.biomeHeight = defaultBiomeRange.biomeHeight;
		this.biomeVolatility = defaultBiomeRange.biomeVolatility;

		this.worldGeneratorTrees = new WorldGenTreesCube( false );
		this.worldGeneratorBigTree = new WorldGenBigTreeCube( false );
		this.worldGeneratorSwamp = new WorldGenSwampCube();

		biomeList[biomeID] = this;
		this.theBiomeDecorator = this.createBiomeDecorator();
	}

	protected CubeBiomeGenBase setAlternateData( AlternateWorldGenData data )
	{
		this.data = data;
		return this;
	}

	public CubeBiomeDecorator decorator()
	{
		return (CubeBiomeDecorator)this.theBiomeDecorator;
	}

	//some getters because these variables are "protected" and not accessible from other packages
	public boolean getEnableRain()
	{
		return this.enableRain;
	}

	protected List getSpawnableCreatureList()
	{
		return this.spawnableCreatureList;
	}

	protected List getSpawnableMonsterList()
	{
		return this.spawnableMonsterList;
	}

	protected List getSpawnableCaveCreatureList()
	{
		return this.spawnableCaveCreatureList;
	}

	protected List getSpawnableWaterCreatureList()
	{
		return this.spawnableWaterCreatureList;
	}

	/**
	 * Allocate a new CubeBiomeDecorator for this BiomeGenBase
	 */
	@Override
	protected CubeBiomeDecorator createBiomeDecorator()
	{
		return new CubeBiomeDecorator();
	}

	/**
	 * Sets the temperature and rainfall of this biome.
	 */
	protected CubeBiomeGenBase setTemperatureAndRainfall( float temp, float rainfall )
	{
		return (CubeBiomeGenBase)super.setTemperatureRainfall( temp, rainfall );
	}

	protected final CubeBiomeGenBase setHeightRange( CubeBiomeGenBase.Height range )
	{
		this.biomeHeight = range.biomeHeight;
		this.biomeVolatility = range.biomeVolatility;
		return this;
	}

	/**
	 * Disable the rain for the biome.
	 */
	@Override
	protected CubeBiomeGenBase setDisableRain()
	{
		return (CubeBiomeGenBase)super.setDisableRain();
	}

	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		return rand.nextInt( 10 ) == 0 ? this.worldGeneratorBigTree : this.worldGeneratorTrees;
	}

	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	@Override
	public WorldGeneratorCube getRandomWorldGenForGrass( Random par1Random )
	{
		return new WorldGenTallGrassCube( Blocks.tallgrass, 1 );
	}

	public String spawnFlower( Random rand, int p_150572_2_, int p_150572_3_, int p_150572_4_ )
	{
		return rand.nextInt( 3 ) > 0 ? BlockFlower.field_149858_b[0] : BlockFlower.field_149859_a[0];
	}

	/**
	 * sets enableSnow to true during biome initialization. returns BiomeGenBase.
	 */
	@Override
	protected CubeBiomeGenBase setEnableSnow()
	{
		return (CubeBiomeGenBase)super.setEnableSnow();
	}

	@Override
	protected CubeBiomeGenBase setBiomeName( String name )
	{
		return (CubeBiomeGenBase)super.setBiomeName( name );
	}

	@Override
	protected CubeBiomeGenBase func_76733_a( int par1 )
	{
		return (CubeBiomeGenBase)super.func_76733_a( par1 );
	}

	@Override
	protected CubeBiomeGenBase setColor( int par1 )
	{
		return (CubeBiomeGenBase)super.setColor( par1 );
	}

	@Override
	protected CubeBiomeGenBase func_150563_c( int p_150563_1_ )
	{
		return (CubeBiomeGenBase)super.func_150563_c( p_150563_1_ );
	}

	@Override
	protected CubeBiomeGenBase func_150557_a( int p_150557_1_, boolean p_150557_2_ )
	{
		return (CubeBiomeGenBase)super.func_150557_a( p_150557_1_, p_150557_2_ );
	}

	public void decorate( World world, Random rand, int cubeX, int cubeY, int cubeZ )
	{
		((CubeBiomeDecorator)this.theBiomeDecorator).decorate( world, rand, this, cubeX, cubeY, cubeZ );
	}

	@Override
	public void func_150573_a( World p_150573_1_, Random p_150573_2_, Block[] p_150573_3_, byte[] p_150573_4_, int p_150573_5_, int p_150573_6_, double p_150573_7_ )
	{
		throw new UnsupportedOperationException( "CubeBiomeGenBase can't replace blocks in columns!" );
	}

	public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		this.replaceBlocks_do( world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, depthNoiseValue );
	}

	public final void replaceBlocks_do( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		Block topBlock = this.topBlock;
		byte metadata = (byte)(this.field_150604_aj & 255);
		Block fillerBlock = this.fillerBlock;

		//How many biome blocks left to set in column? Initially -1
		int numBlocksToChange = -1;

		//Biome blocks depth in current block column. 0 for negative values.
		int depth = (int)(depthNoiseValue / 3.0D + 3.0D + rand.nextDouble() * 0.25D);

		//TODO:
		/*
		 *Default BuildDepth is 8,388,608. the Earth has a radius of ~6,378,100m. Not too far off.
		 * Let's make this world similar to the earth!
		 *
		 *	Crust - 0 to 35km (varies between 5 and 70km thick due to the sea and mountains)
		 *	Upper Mesosphere - 35km to 660km
		 *	Lower Mesosphere - 660km to 2890km
		 *	Outer Core - 2890km to 5150km
		 *	Inner Core - 5150km to 6360km - apparently, the innermost sections of the core could be a plasma! Crazy!
		 *
		 if( yAbs <= BuildSizeEvent.getBuildDepth() + 16 + rand.nextInt( 16 ) ) // generate bedrock in the very bottom cube and below plus random bedrock in the cube above that
		 {
		 cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.bedrock );
		 }
		 else if( yAbs < -32768 + rand.nextInt( 256 ) ) // generate lava sea under y = -32768, plus a rough surface. this is pretty fucking deep though, so nobody will reach this, probably.
		 {
		 cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.lava );
		 }
		 else
		 */
		for( int yAbs = top; yAbs >= bottom; --yAbs )
		{
			//Current block
			Block block = replaceBlocks_getBlock( cube, above, xAbs, yAbs, zAbs );

			//Set numBlocksToChange to -1 when we reach air, skip everything else
			if( block == null || block.getMaterial() == Material.air )
			{
				numBlocksToChange = -1;
				continue;
			}

			//Do not replace any blocks except already replaced and stone
			if( block != Blocks.stone && block != topBlock && block != fillerBlock && block != Blocks.sandstone )
			{
				continue;
			}

			boolean canSetBlock = yAbs <= alterationTop;

			//If we are 1 block below air...
			if( numBlocksToChange == -1 )
			{
				//If depth is <= 0 - only stone
				if( depth <= 0 )
				{
					topBlock = null;
					metadata = 0;
					fillerBlock = Blocks.stone;
				}
				//If we are above or at 4 block under water and at or below one block above water
				else if( yAbs >= seaLevel - 4 && yAbs <= seaLevel + 1 )
				{
					topBlock = this.topBlock;
					metadata = (byte)(this.field_150604_aj & 255);
					fillerBlock = this.fillerBlock;
				}

				//If top block is air and we are below sea level use water instead
				if( yAbs < seaLevel && (topBlock == null || topBlock.getMaterial() == Material.air) )
				{
					if( this.getFloatTemperature( xAbs, yAbs, zAbs ) < 0.15F )
					{
						//or ice if it's cold
						topBlock = Blocks.ice;
						metadata = 0;
					}
					else
					{
						topBlock = Blocks.water;
						metadata = 0;
					}
				}

				//Set num blocks to change to current depth.
				numBlocksToChange = depth;

				if( yAbs >= seaLevel - 1 )
				{
					//If we are above sea level
					replaceBlocks_setBlock( topBlock, metadata, cube, xAbs, yAbs, zAbs, canSetBlock );
				}
				else if( yAbs < seaLevel - 7 - depth )
				{
					//gravel beaches?
					topBlock = null;
					fillerBlock = Blocks.stone;
					replaceBlocks_setBlock( Blocks.gravel, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
				}
				else
				{
					//no grass below sea level
					replaceBlocks_setBlock( fillerBlock, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
				}

				continue;
			}

			// Nothing left to do...
			// so continue
			if( numBlocksToChange <= 0 )
			{
				continue;
			}
			//Decrease blocks to change
			--numBlocksToChange;
			replaceBlocks_setBlock( fillerBlock, 0, cube, xAbs, yAbs, zAbs, canSetBlock );

			// random sandstone generation
			if( numBlocksToChange == 0 && fillerBlock == Blocks.sand )
			{
				numBlocksToChange = rand.nextInt( 4 ) + Math.max( 0, yAbs - 63 );
				fillerBlock = Blocks.sandstone;
			}
		}
	}

	protected final void replaceBlocks_setBlock( Block block, int metadata, Cube cube, int xAbs, int yAbs, int zAbs, boolean reallySet )
	{
		//Modify blocks only if we are at or below alteration top
		if( !reallySet )
		{
			return;
		}
		assert Coords.blockToCube( yAbs ) == cube.getY() || Coords.blockToCube( yAbs ) == cube.getY() - 1;

		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;

		assert Coords.blockToCube( yAbs ) == cube.getY();
		cube.setBlockForGeneration( xRel, yRel, zRel, block, metadata );
	}

	protected final Block replaceBlocks_getBlock( Cube cube, Cube above, int xAbs, int yAbs, int zAbs )
	{
		assert ((CubeWorldServer)cube.getWorld()).getCubeProvider().cubeExists( Coords.blockToCube( xAbs ), Coords.blockToCube( yAbs ), Coords.blockToCube( zAbs ) );

		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;

		if( Coords.blockToCube( yAbs ) == cube.getY() ) // check if we're in the same cube as Cube
		{
			//If we are in the same cube
			return cube.getBlock( xRel, yRel, zRel );
		}
		else
		{
			//we are in cube above
			assert Coords.blockToCube( yAbs ) == cube.getY() + 1;
			return above.getBlock( xRel, yRel, zRel );
		}
	}

	@Override
	protected CubeBiomeGenBase func_150566_k()
	{
		return createAndReturnMutated();
	}

	protected CubeBiomeGenBase createAndReturnMutated()
	{
		return new BiomeGenMutated( this.biomeID + 128, this );
	}

	@Override
	public Class<? extends CubeBiomeGenBase> func_150562_l()
	{
		return this.getClass();
	}

	/*public static CubeBiomeGenBase[] getBiomeGenArray()
	 {
	 return biomeList;
	 }*/
	public static CubeBiomeGenBase getBiome( int val )
	{
		if( val >= 0 && val <= biomeList.length && biomeList[val] != null )
		{
			return (CubeBiomeGenBase)biomeList[val];
		}
		else
		{
			logger.warn( "Biome ID is invalid or out of bounds: " + val + ", defaulting to 0 (Ocean)" );
			return ocean;
		}
	}

	public AlternateWorldGenData getGenData()
	{
		return data;
	}

	static
	{
		plains.createAndReturnMutated();
		desert.createAndReturnMutated();
		forest.createAndReturnMutated();
		taiga.createAndReturnMutated();
		swampland.createAndReturnMutated();
		icePlains.createAndReturnMutated();
		jungle.createAndReturnMutated();
		jungleEdge.createAndReturnMutated();
		coldTaiga.createAndReturnMutated();
		savanna.createAndReturnMutated();
		SavannaPlateau.createAndReturnMutated();
		mesa.createAndReturnMutated();
		mesaPlateauF.createAndReturnMutated();
		mesaPlateau.createAndReturnMutated();
		birchForest.createAndReturnMutated();
		birchForestHills.createAndReturnMutated();
		roofedForest.createAndReturnMutated();
		megaTaiga.createAndReturnMutated();
		extremeHills.createAndReturnMutated();
		extremeHillsPlus.createAndReturnMutated();
		biomeList[megaTaigaHills.biomeID + 128] = biomeList[megaTaiga.biomeID + 128];
		BiomeGenBase[] array = biomeList;
		int var1 = array.length;

		for( int var2 = 0; var2 < var1; ++var2 )
		{
			CubeBiomeGenBase biome = (CubeBiomeGenBase)array[var2];

			if( biome != null && biome.biomeID < 128 )
			{
				field_150597_n.add( biome );
			}
		}

		field_150597_n.remove( hell );
		field_150597_n.remove( sky );
		worldGenDoublePlant = new WorldGenDoublePlantCube();
	}

	public static class Height
	{
		public float biomeHeight;
		public float biomeVolatility;

		public Height( float p_i45371_1_, float p_i45371_2_ )
		{
			this.biomeHeight = p_i45371_1_;
			this.biomeVolatility = p_i45371_2_;
		}

		public CubeBiomeGenBase.Height func_150775_a()
		{
			return new CubeBiomeGenBase.Height( this.biomeHeight * 0.8F, this.biomeVolatility * 0.6F );
		}
	}

	public static class AlternateWorldGenData
	{
		public float minVolatility, maxVolatility;
		public float minHeight, maxHeight, maxHeightDiff;
		public float minTemp, maxTemp;
		public float minRainfall, maxRainfall;
	}
}
