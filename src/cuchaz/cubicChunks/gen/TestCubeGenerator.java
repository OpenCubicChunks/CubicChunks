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
package cuchaz.cubicChunks.gen;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import cuchaz.cubicChunks.gen.lib.exception.ExceptionInvalidParam;
import cuchaz.cubicChunks.gen.lib.module.ModuleBase;
import cuchaz.cubicChunks.gen.lib.module.ScaleBias;
import cuchaz.cubicChunks.gen.lib.module.Simplex;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;
import cuchaz.cubicChunks.world.biome.CubeBiomeGenBase;
import cuchaz.cubicChunks.world.biome.WorldColumnManager;

public class TestCubeGenerator implements ICubeGenerator
{
	private World m_world;
	private boolean m_mapFeaturesEnabled;
	private WorldType m_worldType;
	
	private WorldColumnManager worldColumnManager;
	
	private Random m_rand;
	private CubeBiomeGenBase[] m_biomes;
	private CubeBlocks m_blocks;
	
	private MapGenBase m_caveGenerator;
	private MapGenStronghold m_strongholdGenerator;
	private MapGenVillage m_villageGenerator;
	private MapGenMineshaft m_mineshaftGenerator;
	private MapGenScatteredFeature m_scatteredFeatureGenerator;
	private MapGenBase m_ravineGenerator;
	
	private float[] m_filter5x5;
	
	private NoiseGeneratorPerlin m_biomeNoiseGen;
	private NoiseGeneratorOctaves m_terrainNoiseGenLow;
	private NoiseGeneratorOctaves m_terrainNoiseGenHigh;
	private NoiseGeneratorOctaves m_terrainNoiseGenT;
	private NoiseGeneratorOctaves m_terrainNoiseGenXZ;
	
	private double[] m_biomeNoise;
	private double[] m_terrainNoise;
	private double[] m_terrainNoiseLow;
	private double[] m_terrainNoiseHigh;
	private double[] m_terrainNoiseT;
	private double[] m_terrainNoiseXZ;
	
	private ModuleBase finalBuild;
	
	public TestCubeGenerator( World world )
	{
		m_world = world;
		m_mapFeaturesEnabled = world.getWorldInfo().isMapFeaturesEnabled();
		m_worldType = world.getWorldInfo().getTerrainType();
		
		m_rand = new Random( world.getSeed() );
		m_biomes = null;
		m_blocks = new CubeBlocks();
		
		m_caveGenerator = new MapGenCaves();
		m_strongholdGenerator = new MapGenStronghold();
		m_villageGenerator = new MapGenVillage();
		m_mineshaftGenerator = new MapGenMineshaft();
		m_scatteredFeatureGenerator = new MapGenScatteredFeature();
		m_ravineGenerator = new MapGenRavine();
		
		worldColumnManager = new WorldColumnManager(this.m_world);
		
		m_biomeNoiseGen = new NoiseGeneratorPerlin( m_rand, 4 );
		m_terrainNoiseGenLow = new NoiseGeneratorOctaves( m_rand, 16 );
		m_terrainNoiseGenHigh = new NoiseGeneratorOctaves( m_rand, 16 );
		m_terrainNoiseGenT = new NoiseGeneratorOctaves( m_rand, 8 );
		m_terrainNoiseGenXZ = new NoiseGeneratorOctaves( m_rand, 16 );
		
		m_terrainNoiseLow = null;
		m_terrainNoiseHigh = null;
		m_terrainNoiseT = null;
		m_terrainNoiseXZ = null;
		m_terrainNoise = new double[5*5*3];
		m_biomeNoise = new double[256];
		
		m_terrainNoiseXZ = null;
		
		// init the 5x5 filter
		m_filter5x5 = new float[25];
		for( int i=-2; i<=2; i++ )
		{
			for( int j=-2; j<=2; j++ )
			{
				m_filter5x5[i + 2 + ( j + 2 )*5] = 10.0F/MathHelper.sqrt_float( 0.2F + ( i*i + j*j ) );
			}
		}
		
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(0);
		baseContinentDef_pe0.setFrequency(1.0);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(2.2089);
		baseContinentDef_pe0.setOctaveCount(11);
		baseContinentDef_pe0.setUp();
		
		ScaleBias scaleBias = new ScaleBias(baseContinentDef_pe0);
		scaleBias.setScale(1024);
		scaleBias.setBias(63);
		
		finalBuild = scaleBias;
	}
	
	@Override
	public Column generateColumn( int cubeX, int cubeZ )
	{
		// generate biome info. This is a hackjob.
		m_biomes = worldColumnManager.loadBlockGeneratorData(
			m_biomes,
			Coords.cubeToMinBlock( cubeX ), Coords.cubeToMinBlock( cubeZ ),
			16, 16
		);
		
		return new Column( m_world, cubeX, cubeZ, m_biomes );
	}
	
	@Override
	public Cube generateCube( Column column, int cubeX, int cubeY, int cubeZ )
	{
		// init random
		m_rand.setSeed( (long)cubeX * 341873128712L + (long)cubeZ * 132897987541L );
		
		// get more biome data
		// NOTE: this is different from the column biome data for some reason...
		m_biomes = (CubeBiomeGenBase[]) worldColumnManager.getBiomesForGeneration(
			m_biomes,
			cubeX * 4 - 2, cubeZ * 4 - 2,
			16, 16
		);
		
		// actually generate the terrain
		m_blocks.clear();
		generateTerrain( cubeX, cubeY, cubeZ, m_blocks );
		
		// is there nothing but air here?
		if( m_blocks.isEmpty() )
		{
			return Cube.generateEmptyCubeAndAddToColumn( m_world, column, cubeX, cubeY, cubeZ );
		}
		
//		replaceBlocksForBiome( cubeX, cubeY, cubeZ, m_blocks.m_blocks, m_blocks.m_meta, m_biomes );
//		replaceBlocksForBiome( cubeX, cubeY, cubeZ, m_blocks, m_biomes );
		
		/*
		// generate world features
		m_caveGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
		m_ravineGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
		if( m_mapFeaturesEnabled )
		{
			m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
			m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeZ, m_blocks );
		}
		*/
		
		return Cube.generateCubeAndAddToColumn( m_world, column, cubeX, cubeY, cubeZ, m_blocks );
	}
	
	private void generateTerrain( int cubeX, int cubeY, int cubeZ, CubeBlocks blocks )
	{	
		// UNDONE: centralize sea level somehow
		final int seaLevel = 63;
						
		for( int xRel=0; xRel < 16; xRel++ )
		{
			int xAbs = cubeX << 4 | xRel;
			
			for( int zRel=0; zRel < 16; zRel++ )
			{				
				int zAbs = cubeZ << 4 | zRel;
				
				for( int yRel=0; yRel < 16; yRel++ )
				{
					int yAbs = Coords.localToBlock( cubeY, yRel );
					
					double val = finalBuild.getValue(xAbs, yAbs, zAbs);
					val -= yAbs;
					
					if( val > 0.0D )
					{
						blocks.setBlock( xRel, yRel, zRel, Blocks.stone );
					}
					else if( yAbs < seaLevel )
					{
						blocks.setBlock( xRel, yRel, zRel, Blocks.water );
					}
					else
					{
						blocks.setBlock( xRel, yRel, zRel, null );
					}
				}		
			}
		}
	}
	
	private void replaceBlocksForBiome( int cubeX, int cubeY, int cubeZ, CubeBlocks cubeBlocks, CubeBiomeGenBase[] biomes )
	{
		m_biomeNoise = m_biomeNoiseGen.func_151599_a( m_biomeNoise, Coords.cubeToMinBlock( cubeX ), Coords.cubeToMinBlock( cubeZ ), 16, 16, 16, 16, 1 );
		
		for( int xRel = 0; xRel < 16; xRel++ )
		{
			int xAbs = cubeX << 4 | xRel;
			
			for( int zRel = 0; zRel < 16; zRel++ )
			{
				int zAbs = cubeZ << 4 | zRel;
				
				int xzCoord = zRel | xRel << 4;

				for (int yRel = 0; yRel < 16; yRel++)
				{	
					int yAbs = cubeY << 4 | yRel;
					
//					if ( biomes[xzCoord] == null )
//					{
//						System.out.println(biomes[xzCoord] + ":" + m_world + ":" + m_rand + ":" + cubeBlocks );
//					}
					
//					biomes[xzCoord].modifyBlocks_pre(
//							m_world, m_rand,
//							cubeBlocks,
//							xAbs, yAbs, zAbs,
//							m_biomeNoise[xzCoord]
//						);
				}
			}
		}
	}
	
	@Override
	public void populate( ICubeGenerator generator, int cubeX, int cubeY, int cubeZ )
	{
		/* TEMP: don't do population yet
		
        // field_149832_M = fallInstantly
		BlockFalling.field_149832_M = true;
		int xAbs = cubeX * 16;
		int yAbs = cubeY * 16;
		int zAbs = cubeZ * 16;
		BiomeGenBase var6 = m_world.getBiomeGenForCoords( xAbs + 16, zAbs + 16 );
		m_rand.setSeed( m_world.getSeed() );
		long var7 = m_rand.nextLong() / 2L * 2L + 1L;
		long var9 = m_rand.nextLong() / 2L * 2L + 1L;
		m_rand.setSeed( (long)cubeX * var7 + (long)cubeZ * var9 ^ m_world.getSeed() );
		boolean var11 = false;
		
		if( m_mapFeaturesEnabled )
		{
			m_mineshaftGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			var11 = m_villageGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			m_strongholdGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			m_scatteredFeatureGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		}
		
		int var12;
		int var13;
		int var14;
		
		if( var6 != BiomeGenBase.desert && var6 != BiomeGenBase.desertHills && !var11 && m_rand.nextInt( 4 ) == 0 )
		{
			var12 = xAbs + m_rand.nextInt( 16 ) + 8;
			var13 = m_rand.nextInt( 256 ); // randomly picks a y value
			var14 = zAbs + m_rand.nextInt( 16 ) + 8;
			( new WorldGenLakes( Blocks.water ) ).generate( m_world, m_rand, var12, var13, var14 );
		}
		
		if( !var11 && m_rand.nextInt( 8 ) == 0 )
		{
			var12 = xAbs + m_rand.nextInt( 16 ) + 8;
			var13 = m_rand.nextInt( m_rand.nextInt( 248 ) + 8 );
			var14 = zAbs + m_rand.nextInt( 16 ) + 8;
			
			if( var13 < 63 || m_rand.nextInt( 10 ) == 0 )
			{
				( new WorldGenLakes( Blocks.lava ) ).generate( m_world, m_rand, var12, var13, var14 );
			}
		}
		
		for( var12 = 0; var12 < 8; ++var12 ) //really? instead of making a new var here, they reuse var12 and make a new var15 instead of how they were doing it the previous two times
		{
			var13 = xAbs + m_rand.nextInt( 16 ) + 8;
			var14 = m_rand.nextInt( 256 );
			int var15 = zAbs + m_rand.nextInt( 16 ) + 8;
			( new WorldGenDungeons() ).generate( m_world, m_rand, var13, var14, var15 );
		}
		
		var6.decorate( m_world, m_rand, xAbs, zAbs );
		SpawnerAnimals.performWorldGenSpawning( m_world, var6, xAbs + 8, zAbs + 8, 16, 16, m_rand );
		xAbs += 8;
		zAbs += 8;
		
		for( var12 = 0; var12 < 16; ++var12 )
		{
			for( var13 = 0; var13 < 16; ++var13 )
			{
				var14 = m_world.getPrecipitationHeight( xAbs + var12, zAbs + var13 );
				
				if( m_world.isBlockFreezable( var12 + xAbs, var14 - 1, var13 + zAbs ) )
				{
					m_world.setBlock( var12 + xAbs, var14 - 1, var13 + zAbs, Blocks.ice, 0, 2 );
				}
				
				if( m_world.func_147478_e( var12 + xAbs, var14, var13 + zAbs, true ) )
				{
					m_world.setBlock( var12 + xAbs, var14, var13 + zAbs, Blocks.snow_layer, 0, 2 );
				}
			}
		}
		
		BlockFalling.field_149832_M = false;
		*/
	}
	
	@Override
	public List<SpawnListEntry> getPossibleCreatures( EnumCreatureType par1EnumCreatureType, int cubeX, int cubeY, int cubeZ )
	{
		CubeBiomeGenBase var5 = (CubeBiomeGenBase) m_world.getBiomeGenForCoords( cubeX, cubeZ );
		return par1EnumCreatureType == EnumCreatureType.monster && m_scatteredFeatureGenerator.func_143030_a( cubeX, cubeY, cubeZ ) 
				? m_scatteredFeatureGenerator.getScatteredFeatureSpawnList()
				: var5.getSpawnableList( par1EnumCreatureType );
	}
	
	@Override
	public ChunkPosition getNearestStructure( World world, String structureType, int blockX, int blockY, int blockZ )
	{
		if( "Stronghold".equals( structureType ) && m_strongholdGenerator != null )
		{
			return m_strongholdGenerator.func_151545_a( world, blockX, blockY, blockZ );
		}
		return null;
	}
	
	@Override
	public void recreateStructures( int cubeX, int cubeY, int cubeZ )
	{
		// disable this for now until these things can be cubified
		if( false )
		{
			if( m_mapFeaturesEnabled )
			{
				m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
				m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
				m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
				m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeY, (Block[])null );
			}
		}
	}
}
