package cuchaz.cubicChunks.gen;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public class CubeGenerator implements ICubeGenerator
{
	// vars I actually understand
	private World m_world;
	private final boolean m_mapFeaturesEnabled;
	private WorldType m_worldType;
	
	private Random m_rand;
	private BiomeGenBase[] m_biomes;
	
	private MapGenBase m_caveGenerator = new MapGenCaves();
	private MapGenStronghold m_strongholdGenerator = new MapGenStronghold();
	private MapGenVillage m_villageGenerator = new MapGenVillage();
	private MapGenMineshaft m_mineshaftGenerator = new MapGenMineshaft();
	private MapGenScatteredFeature m_scatteredFeatureGenerator = new MapGenScatteredFeature();
	private MapGenBase m_ravineGenerator = new MapGenRavine();
	
	private final float[] m_filter5x5;
	
	private double[] m_noise1;
	private double[] m_noise2;
	private double[] m_noise3;
	private double[] m_noise4;
	private double[] m_noise6;
	private final double[] m_terrainNoise;
	
	// what the hell do these do?
	private NoiseGeneratorOctaves noiseGen1;
	private NoiseGeneratorOctaves noiseGen2;
	private NoiseGeneratorOctaves noiseGen3;
	private NoiseGeneratorPerlin noiseGen4;
	private NoiseGeneratorOctaves noiseGen6;
	
	public CubeGenerator( World world )
	{
		m_world = world;
		m_mapFeaturesEnabled = world.getWorldInfo().isMapFeaturesEnabled();
		m_worldType = world.getWorldInfo().getTerrainType();
		m_rand = new Random( world.getSeed() );
		
		noiseGen1 = new NoiseGeneratorOctaves( m_rand, 16 );
		noiseGen2 = new NoiseGeneratorOctaves( m_rand, 16 );
		noiseGen3 = new NoiseGeneratorOctaves( m_rand, 8 );
		noiseGen4 = new NoiseGeneratorPerlin( m_rand, 4 );
		noiseGen6 = new NoiseGeneratorOctaves( m_rand, 16 );
		m_terrainNoise = new double[825];
		m_noise4 = new double[256];
		
		// init the 5x5 filter
		m_filter5x5 = new float[25];
		for( int i=-2; i<=2; i++ )
		{
			for( int j=-2; j<=2; j++ )
			{
				m_filter5x5[i + 2 + ( j + 2 )*5] = 10.0F/MathHelper.sqrt_float( 0.2F + ( i*i + j*j ) );
			}
		}
	}
	
	@Override
	public Column generateColumn( int cubeX, int cubeZ )
	{
		// generate biome info
		m_biomes = m_world.getWorldChunkManager().loadBlockGeneratorData(
			m_biomes,
			Coords.cubeToMinBlock( cubeX ), Coords.cubeToMinBlock( cubeZ ),
			16, 16
		);
		
		return new Column( m_world, cubeX, cubeZ, m_biomes );
	}
	
	@Override
	public Cube generateCube( Column column, int cubeX, int cubeY, int cubeZ )
	{
		// UNDONE: still need to cubify this method
		
		Block[] blocks = new Block[65536];
		byte[] meta = new byte[65536];
		
		// init random
		m_rand.setSeed( (long)cubeX * 341873128712L + (long)cubeZ * 132897987541L );
		
		// get more biome data
		// NOTE: this is different from the column biome data for some reason...
		m_biomes = m_world.getWorldChunkManager().getBiomesForGeneration(
			m_biomes,
			cubeX * 4 - 2, cubeZ * 4 - 2,
			10, 10
		);
		
		// actually generate the terrain
		generateTerrain( cubeX, cubeY, cubeZ, blocks );
		replaceBlocksForBiome( cubeX, cubeY, cubeZ, blocks, meta, m_biomes );
		
		// TEMP: don't generate features for now. Need to cubify those generators
		if( false )
		{
			// generate world features
			m_caveGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
			m_ravineGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
			if( m_mapFeaturesEnabled )
			{
				m_mineshaftGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
				m_villageGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
				m_strongholdGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
				m_scatteredFeatureGenerator.func_151539_a( null, m_world, cubeX, cubeZ, blocks );
			}
		}
		
		// create the cube
		Cube cube = new Cube( m_world, column, cubeX, cubeY, cubeZ, !m_world.provider.hasNoSky );
		
		// UNDONE: set the blocks and meta for the cube
		
		// init sky lighting and height
		// UNDONE: move this out of the generator and into the cube provider?
		column.generateSkylightMap();
		
		return cube;
	}
	
	public void generateTerrain( int cubeX, int cubeY, int cubeZ, Block[] blocks )
	{
		byte seaLevel = 63;
		
		generateNoise( cubeX*4, cubeY*4, cubeZ*4 );
		
		for( int localXBig=0; localXBig<4; localXBig++ )
		{
			int var6 = localXBig * 5;
			int var7 = ( localXBig + 1 ) * 5;
			
			for( int localZBig=0; localZBig<4; localZBig++ )
			{
				int var9 = ( var6 + localZBig ) * 33;
				int var10 = ( var6 + localZBig + 1 ) * 33;
				int var11 = ( var7 + localZBig ) * 33;
				int var12 = ( var7 + localZBig + 1 ) * 33;
				
				for( int blockYBig=0; blockYBig<32; blockYBig++ )
				{
					double var16 = m_terrainNoise[var9 + blockYBig];
					double var18 = m_terrainNoise[var10 + blockYBig];
					double var20 = m_terrainNoise[var11 + blockYBig];
					double var22 = m_terrainNoise[var12 + blockYBig];
					double var24 = ( m_terrainNoise[var9 + blockYBig + 1] - var16 )/8;
					double var26 = ( m_terrainNoise[var10 + blockYBig + 1] - var18 )/8;
					double var28 = ( m_terrainNoise[var11 + blockYBig + 1] - var20 )/8;
					double var30 = ( m_terrainNoise[var12 + blockYBig + 1] - var22 )/8;
					
					for( int blockYSmall=0; blockYSmall<8; blockYSmall++ )
					{
						int blockY = blockYBig*8 + blockYSmall;
						
						double var35 = var16;
						double var37 = var18;
						double var39 = ( var20 - var16 )/4;
						double var41 = ( var22 - var18 )/4;
						
						for( int localXSmall=0; localXSmall<4; localXSmall++ )
						{
							int localX = localXBig*4 + localXSmall;
							
							double var50 = ( var37 - var35 )/4;
							double var48 = var35 - var50;
							
							for( int localZSmall=0; localZSmall<4; localZSmall++ )
							{
								int localZ = localZBig*4 + localZSmall;
								int blockIndex = localX << 12 | localZ << 8 | blockY;
								
								if( ( var48 += var50 ) > 0.0D )
								{
									blocks[blockIndex] = Blocks.stone;
								}
								else if( blockY < seaLevel )
								{
									blocks[blockIndex] = Blocks.water;
								}
								else
								{
									blocks[blockIndex] = null;
								}
							}
							
							var35 += var39;
							var37 += var41;
						}
						
						var16 += var24;
						var18 += var26;
						var20 += var28;
						var22 += var30;
					}
				}
			}
		}
	}
	
	public void replaceBlocksForBiome( int cubeX, int cubeY, int cubeZ, Block[] blocks, byte[] metadata, BiomeGenBase[] biomesForGeneration )
	{
		double var6 = 0.03125D;
		m_noise4 = noiseGen4.func_151599_a( m_noise4, (double)( cubeX * 16 ), (double)( cubeZ * 16 ), 16, 16, var6 * 2.0D, var6 * 2.0D, 1.0D );
		
		for( int xRel = 0; xRel < 16; ++xRel )
		{
			for( int zRel = 0; zRel < 16; ++zRel )
			{
				BiomeGenBase biomeGenBase = biomesForGeneration[zRel + xRel * 16];
				biomeGenBase.func_150573_a( m_world, m_rand, blocks, metadata, cubeX * 16 + xRel, cubeZ * 16 + zRel, m_noise4[zRel + xRel * 16] );
			}
		}
	}
	
	private void generateNoise( int x, int y, int z )
	{
		m_noise1 = noiseGen1.generateNoiseOctaves( m_noise1, x, y, z, 5, 33, 5, 684.412D, 684.412D, 684.412D );
		m_noise2 = noiseGen2.generateNoiseOctaves( m_noise2, x, y, z, 5, 33, 5, 684.412D, 684.412D, 684.412D );
		m_noise3 = noiseGen3.generateNoiseOctaves( m_noise3, x, y, z, 5, 33, 5, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D );
		m_noise6 = noiseGen6.generateNoiseOctaves( m_noise6, x, z, 5, 5, 200.0D, 200.0D, 0.5D );
		
		int var12 = 0;
		int var13 = 0;
		
		for( int u=0; u<5; u++ )
		{
			for( int v=0; v<5; v++ )
			{
				float var18 = 0.0F;
				float var19 = 0.0F;
				float sumHeightScale = 0.0F;
				
				BiomeGenBase uvBiome = m_biomes[u + 2 + ( v + 2 )*10];
				
				// apply 5x5 filter
				for( int i = -2; i <= 2; ++i )
				{
					for( int j = -2; j <= 2; ++j )
					{
						BiomeGenBase ijBiome = m_biomes[u + i + 2 + ( v + j + 2 )*10];
						float minHeight = ijBiome.minHeight;
						float maxHeight = ijBiome.maxHeight;
						
						// if world type is amplified
						if( m_worldType == WorldType.field_151360_e && minHeight > 0.0F )
						{
							minHeight = minHeight*2 + 1;
							maxHeight = maxHeight*4 + 1;
						}
						
						float heightScale = m_filter5x5[i + 2 + ( j + 2 )*5]/( minHeight + 2.0F );
						
						if( ijBiome.minHeight > uvBiome.minHeight )
						{
							heightScale /= 2;
						}
						
						var18 += maxHeight * heightScale;
						var19 += minHeight * heightScale;
						sumHeightScale += heightScale;
					}
				}
				
				var18 /= sumHeightScale;
				var19 /= sumHeightScale;
				var18 = var18 * 0.9F + 0.1F;
				var19 = ( var19 * 4.0F - 1.0F ) / 8.0F;
				double var46 = m_noise6[var13] / 8000.0D;
				
				if( var46 < 0.0D )
				{
					var46 = -var46 * 0.3D;
				}
				
				var46 = var46 * 3.0D - 2.0D;
				
				if( var46 < 0.0D )
				{
					var46 /= 2.0D;
					
					if( var46 < -1.0D )
					{
						var46 = -1.0D;
					}
					
					var46 /= 1.4D;
					var46 /= 2.0D;
				}
				else
				{
					if( var46 > 1.0D )
					{
						var46 = 1.0D;
					}
					
					var46 /= 8.0D;
				}
				
				++var13;
				double var47 = (double)var19;
				double var48 = (double)var18;
				var47 += var46 * 0.2D;
				var47 = var47 * 8.5D / 8.0D;
				double var29 = 8.5D + var47 * 4.0D;
				
				for( int var31 = 0; var31 < 33; ++var31 )
				{
					double var32 = ( (double)var31 - var29 ) * 12.0D * 128.0D / 256.0D / var48;
					
					if( var32 < 0.0D )
					{
						var32 *= 4.0D;
					}
					
					double var34 = m_noise1[var12] / 512.0D;
					double var36 = m_noise2[var12] / 512.0D;
					double var38 = ( m_noise3[var12] / 10.0D + 1.0D ) / 2.0D;
					double var40 = MathHelper.denormalizeClamp( var34, var36, var38 ) - var32;
					
					if( var31 > 29 )
					{
						double var42 = (double)( (float)( var31 - 29 ) / 3.0F );
						var40 = var40 * ( 1.0D - var42 ) + -10.0D * var42;
					}
					
					m_terrainNoise[var12] = var40;
					++var12;
				}
			}
		}
	}
	
	@Override
	public void populate( ICubeGenerator generator, int cubeX, int cubeY, int cubeZ )
	{
		// TEMP: check height
        for( int x = 0; x < 16; x++ )
        {
        	for( int z = 0; z < 16; z++ )
        	{
        		if( m_world.getHeightValue( cubeX << 4 | x, cubeZ << 4 | z ) <= 0 )
        		{
        			throw new Error( "World doesn't know about chunk!" );
        		}
        	}
        }
        
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
	}
	
	@Override
	public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures( EnumCreatureType par1EnumCreatureType, int cubeX, int cubeY, int cubeZ )
	{
		BiomeGenBase var5 = m_world.getBiomeGenForCoords( cubeX, cubeZ );
		return par1EnumCreatureType == EnumCreatureType.monster && m_scatteredFeatureGenerator.func_143030_a( cubeX, cubeY, cubeZ ) 
				? m_scatteredFeatureGenerator.getScatteredFeatureSpawnList()
				: var5.getSpawnableList( par1EnumCreatureType );
	}
	
	public ChunkPosition func_147416_a( World world, String str, int cubeX, int cubeY, int cubeZ )
	{
		return "Stronghold".equals( str ) && m_strongholdGenerator != null ? m_strongholdGenerator.func_151545_a( world, cubeX, cubeY, cubeZ ) : null;
	}
	
	@Override
	public void recreateStructures( int cubeX, int cubeY, int cubeZ )
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
