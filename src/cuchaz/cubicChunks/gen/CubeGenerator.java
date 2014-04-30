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
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;

public class CubeGenerator implements ICubeGenerator
{
	/** RNG. */
	private Random rand;
	private NoiseGeneratorOctaves noiseGen1;
	private NoiseGeneratorOctaves noiseGen2;
	private NoiseGeneratorOctaves noiseGen3;
	private NoiseGeneratorPerlin noiseGen4;
	
	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen5;
	
	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen6;
	public NoiseGeneratorOctaves mobSpawnerNoise;
	
	/** Reference to the World object. */
	private World worldObj;
	
	/** are map structures going to be generated (e.g. strongholds) */
	private final boolean mapFeaturesEnabled;
	private WorldType worldType;
	private final double[] field_147434_q;
	private final float[] field_147433_r;
	private double[] stoneNoise = new double[256];
	private MapGenBase caveGenerator = new MapGenCaves();
	
	/** Holds Stronghold Generator */
	private MapGenStronghold strongholdGenerator = new MapGenStronghold();
	
	/** Holds Village Generator */
	private MapGenVillage villageGenerator = new MapGenVillage();
	
	/** Holds Mineshaft Generator */
	private MapGenMineshaft mineshaftGenerator = new MapGenMineshaft();
	private MapGenScatteredFeature scatteredFeatureGenerator = new MapGenScatteredFeature();
	
	/** Holds ravine generator */
	private MapGenBase ravineGenerator = new MapGenRavine();
	
	/** The biomes that are used to generate the chunk */
	private BiomeGenBase[] biomesForGeneration;
	
	double[] field_147427_d;
	double[] field_147428_e;
	double[] field_147425_f;
	double[] field_147426_g;
	int[][] field_73219_j = new int[32][32];
	
	public CubeGenerator( World world )
	{
		worldObj = world;
		mapFeaturesEnabled = world.getWorldInfo().isMapFeaturesEnabled();
		worldType = world.getWorldInfo().getTerrainType();
		rand = new Random( world.getSeed() );
		noiseGen1 = new NoiseGeneratorOctaves( rand, 16 );
		noiseGen2 = new NoiseGeneratorOctaves( rand, 16 );
		noiseGen3 = new NoiseGeneratorOctaves( rand, 8 );
		noiseGen4 = new NoiseGeneratorPerlin( rand, 4 );
		noiseGen5 = new NoiseGeneratorOctaves( rand, 10 );
		noiseGen6 = new NoiseGeneratorOctaves( rand, 16 );
		mobSpawnerNoise = new NoiseGeneratorOctaves( rand, 8 );
		field_147434_q = new double[825];
		field_147433_r = new float[25];
		
		for( int var5 = -2; var5 <= 2; ++var5 )
		{
			for( int var6 = -2; var6 <= 2; ++var6 )
			{
				float var7 = 10.0F / MathHelper.sqrt_float( (float)( var5 * var5 + var6 * var6 ) + 0.2F );
				field_147433_r[var5 + 2 + ( var6 + 2 ) * 5] = var7;
			}
		}
	}
	
	@Override
	public Column generateColumn( int cubeX, int cubeZ )
	{
		// generate biome info
		// UNDONE: figure out how biome generation works
		// there seems to be lots of caching and middle men
		biomesForGeneration = worldObj.getWorldChunkManager().loadBlockGeneratorData( biomesForGeneration, cubeX * 16, cubeZ * 16, 16, 16 );
		
		Column column = new Column( worldObj, cubeX, cubeZ, biomesForGeneration );
		
		return column;
	}
	
	@Override
	public Cube generateCube( Column column, int cubeX, int cubeY, int cubeZ )
	{
		Block[] blocks = new Block[65536];
		byte[] meta = new byte[65536];
		
		// init random
		rand.setSeed( (long)cubeX * 341873128712L + (long)cubeZ * 132897987541L );
		
		// actually generate the terrain
		generateTerrain( cubeX, cubeY, cubeZ, blocks );
		replaceBlocksForBiome( cubeX, cubeY, cubeZ, blocks, meta, biomesForGeneration );
		
		// generate world features
		caveGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
		ravineGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
		if( mapFeaturesEnabled )
		{
			mineshaftGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
			villageGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
			strongholdGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
			scatteredFeatureGenerator.func_151539_a( null, worldObj, cubeX, cubeZ, blocks );
		}
		
		// create the cube
		Cube cube = new Cube( worldObj, column, cubeX, cubeY, cubeZ, !worldObj.provider.hasNoSky );
		
		// UNDONE: set the blocks and meta for the cube
		
		// init sky lighting and height
		// UNDONE: move this out of the generator and into the cube provider?
		column.generateSkylightMap();
		
		return cube;
	}
	
	public void generateTerrain( int cubeX, int cubeY, int cubeZ, Block[] blocks )
	{
		byte seaLevel = 63;
		biomesForGeneration = worldObj.getWorldChunkManager().getBiomesForGeneration( biomesForGeneration, cubeX * 4 - 2, cubeZ * 4 - 2, 10, 10 );
		func_147423_a( cubeX * 4, cubeY * 4, cubeZ * 4 );
		
		for( int var5 = 0; var5 < 4; ++var5 )
		{
			int var6 = var5 * 5;
			int var7 = ( var5 + 1 ) * 5;
			
			for( int var8 = 0; var8 < 4; ++var8 )
			{
				int var9 = ( var6 + var8 ) * 33;
				int var10 = ( var6 + var8 + 1 ) * 33;
				int var11 = ( var7 + var8 ) * 33;
				int var12 = ( var7 + var8 + 1 ) * 33;
				
				for( int var13 = 0; var13 < 32; ++var13 )
				{
					double var14 = 0.125D;
					double var16 = field_147434_q[var9 + var13];
					double var18 = field_147434_q[var10 + var13];
					double var20 = field_147434_q[var11 + var13];
					double var22 = field_147434_q[var12 + var13];
					double var24 = ( field_147434_q[var9 + var13 + 1] - var16 ) * var14;
					double var26 = ( field_147434_q[var10 + var13 + 1] - var18 ) * var14;
					double var28 = ( field_147434_q[var11 + var13 + 1] - var20 ) * var14;
					double var30 = ( field_147434_q[var12 + var13 + 1] - var22 ) * var14;
					
					for( int var32 = 0; var32 < 8; ++var32 )
					{
						double var33 = 0.25D;
						double var35 = var16;
						double var37 = var18;
						double var39 = ( var20 - var16 ) * var33;
						double var41 = ( var22 - var18 ) * var33;
						
						for( int var43 = 0; var43 < 4; ++var43 )
						{
							int var44 = var43 + var5 * 4 << 12 | 0 + var8 * 4 << 8 | var13 * 8 + var32;
							short var45 = 256;
							var44 -= var45;
							double var46 = 0.25D;
							double var50 = ( var37 - var35 ) * var46;
							double var48 = var35 - var50;
							
							for( int var52 = 0; var52 < 4; ++var52 )
							{
								if( ( var48 += var50 ) > 0.0D )
								{
									blocks[var44 += var45] = Blocks.stone;
								}
								else if( var13 * 8 + var32 < seaLevel )
								{
									blocks[var44 += var45] = Blocks.water;
								}
								else
								{
									blocks[var44 += var45] = null;
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
		stoneNoise = noiseGen4.func_151599_a( stoneNoise, (double)( cubeX * 16 ), (double)( cubeZ * 16 ), 16, 16, var6 * 2.0D, var6 * 2.0D, 1.0D );
		
		for( int xRel = 0; xRel < 16; ++xRel )
		{
			for( int zRel = 0; zRel < 16; ++zRel )
			{
				BiomeGenBase biomeGenBase = biomesForGeneration[zRel + xRel * 16];
				biomeGenBase.func_150573_a( worldObj, rand, blocks, metadata, cubeX * 16 + xRel, cubeZ * 16 + zRel, stoneNoise[zRel + xRel * 16] );
			}
		}
	}
	
	private void func_147423_a( int cubeX, int cubeY, int cubeZ )
	{
		field_147426_g = noiseGen6.generateNoiseOctaves( field_147426_g, cubeX, cubeZ, 5, 5, 200.0D, 200.0D, 0.5D );
		field_147427_d = noiseGen3.generateNoiseOctaves( field_147427_d, cubeX, cubeY, cubeZ, 5, 33, 5, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D );
		field_147428_e = noiseGen1.generateNoiseOctaves( field_147428_e, cubeX, cubeY, cubeZ, 5, 33, 5, 684.412D, 684.412D, 684.412D );
		field_147425_f = noiseGen2.generateNoiseOctaves( field_147425_f, cubeX, cubeY, cubeZ, 5, 33, 5, 684.412D, 684.412D, 684.412D );
		int var12 = 0;
		int var13 = 0;
		
		for( int var16 = 0; var16 < 5; ++var16 )
		{
			for( int var17 = 0; var17 < 5; ++var17 )
			{
				float var18 = 0.0F;
				float var19 = 0.0F;
				float var20 = 0.0F;
				byte var21 = 2;
				BiomeGenBase var22 = biomesForGeneration[var16 + 2 + ( var17 + 2 ) * 10];
				
				for( int var23 = -var21; var23 <= var21; ++var23 )
				{
					for( int var24 = -var21; var24 <= var21; ++var24 )
					{
						BiomeGenBase var25 = biomesForGeneration[var16 + var23 + 2 + ( var17 + var24 + 2 ) * 10];
						float var26 = var25.minHeight;
						float var27 = var25.maxHeight;
						
						if( worldType == WorldType.field_151360_e && var26 > 0.0F )
						{
							var26 = 1.0F + var26 * 2.0F;
							var27 = 1.0F + var27 * 4.0F;
						}
						
						float var28 = field_147433_r[var23 + 2 + ( var24 + 2 ) * 5] / ( var26 + 2.0F );
						
						if( var25.minHeight > var22.minHeight )
						{
							var28 /= 2.0F;
						}
						
						var18 += var27 * var28;
						var19 += var26 * var28;
						var20 += var28;
					}
				}
				
				var18 /= var20;
				var19 /= var20;
				var18 = var18 * 0.9F + 0.1F;
				var19 = ( var19 * 4.0F - 1.0F ) / 8.0F;
				double var46 = field_147426_g[var13] / 8000.0D;
				
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
					
					double var34 = field_147428_e[var12] / 512.0D;
					double var36 = field_147425_f[var12] / 512.0D;
					double var38 = ( field_147427_d[var12] / 10.0D + 1.0D ) / 2.0D;
					double var40 = MathHelper.denormalizeClamp( var34, var36, var38 ) - var32;
					
					if( var31 > 29 )
					{
						double var42 = (double)( (float)( var31 - 29 ) / 3.0F );
						var40 = var40 * ( 1.0D - var42 ) + -10.0D * var42;
					}
					
					field_147434_q[var12] = var40;
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
        		if( worldObj.getHeightValue( cubeX << 4 | x, cubeZ << 4 | z ) <= 0 )
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
		BiomeGenBase var6 = worldObj.getBiomeGenForCoords( xAbs + 16, zAbs + 16 );
		rand.setSeed( worldObj.getSeed() );
		long var7 = rand.nextLong() / 2L * 2L + 1L;
		long var9 = rand.nextLong() / 2L * 2L + 1L;
		rand.setSeed( (long)cubeX * var7 + (long)cubeZ * var9 ^ worldObj.getSeed() );
		boolean var11 = false;
		
		if( mapFeaturesEnabled )
		{
			mineshaftGenerator.generateStructuresInChunk( worldObj, rand, cubeX, cubeZ );
			var11 = villageGenerator.generateStructuresInChunk( worldObj, rand, cubeX, cubeZ );
			strongholdGenerator.generateStructuresInChunk( worldObj, rand, cubeX, cubeZ );
			scatteredFeatureGenerator.generateStructuresInChunk( worldObj, rand, cubeX, cubeZ );
		}
		
		int var12;
		int var13;
		int var14;
		
		if( var6 != BiomeGenBase.desert && var6 != BiomeGenBase.desertHills && !var11 && rand.nextInt( 4 ) == 0 )
		{
			var12 = xAbs + rand.nextInt( 16 ) + 8;
			var13 = rand.nextInt( 256 ); // randomly picks a y value
			var14 = zAbs + rand.nextInt( 16 ) + 8;
			( new WorldGenLakes( Blocks.water ) ).generate( worldObj, rand, var12, var13, var14 );
		}
		
		if( !var11 && rand.nextInt( 8 ) == 0 )
		{
			var12 = xAbs + rand.nextInt( 16 ) + 8;
			var13 = rand.nextInt( rand.nextInt( 248 ) + 8 );
			var14 = zAbs + rand.nextInt( 16 ) + 8;
			
			if( var13 < 63 || rand.nextInt( 10 ) == 0 )
			{
				( new WorldGenLakes( Blocks.lava ) ).generate( worldObj, rand, var12, var13, var14 );
			}
		}
		
		for( var12 = 0; var12 < 8; ++var12 ) //really? instead of making a new var here, they reuse var12 and make a new var15 instead of how they were doing it the previous two times
		{
			var13 = xAbs + rand.nextInt( 16 ) + 8;
			var14 = rand.nextInt( 256 );
			int var15 = zAbs + rand.nextInt( 16 ) + 8;
			( new WorldGenDungeons() ).generate( worldObj, rand, var13, var14, var15 );
		}
		
		var6.decorate( worldObj, rand, xAbs, zAbs );
		SpawnerAnimals.performWorldGenSpawning( worldObj, var6, xAbs + 8, zAbs + 8, 16, 16, rand );
		xAbs += 8;
		zAbs += 8;
		
		for( var12 = 0; var12 < 16; ++var12 )
		{
			for( var13 = 0; var13 < 16; ++var13 )
			{
				var14 = worldObj.getPrecipitationHeight( xAbs + var12, zAbs + var13 );
				
				if( worldObj.isBlockFreezable( var12 + xAbs, var14 - 1, var13 + zAbs ) )
				{
					worldObj.setBlock( var12 + xAbs, var14 - 1, var13 + zAbs, Blocks.ice, 0, 2 );
				}
				
				if( worldObj.func_147478_e( var12 + xAbs, var14, var13 + zAbs, true ) )
				{
					worldObj.setBlock( var12 + xAbs, var14, var13 + zAbs, Blocks.snow_layer, 0, 2 );
				}
			}
		}
		
		BlockFalling.field_149832_M = false;
	}
	
	@Override
	public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures( EnumCreatureType par1EnumCreatureType, int cubeX, int cubeY, int cubeZ )
	{
		BiomeGenBase var5 = worldObj.getBiomeGenForCoords( cubeX, cubeZ );
		return par1EnumCreatureType == EnumCreatureType.monster && scatteredFeatureGenerator.func_143030_a( cubeX, cubeY, cubeZ ) 
				? scatteredFeatureGenerator.getScatteredFeatureSpawnList()
				: var5.getSpawnableList( par1EnumCreatureType );
	}
	
	public ChunkPosition func_147416_a( World world, String str, int cubeX, int cubeY, int cubeZ )
	{
		return "Stronghold".equals( str ) && strongholdGenerator != null ? strongholdGenerator.func_151545_a( world, cubeX, cubeY, cubeZ ) : null;
	}
	
	@Override
	public void recreateStructures( int cubeX, int cubeY, int cubeZ )
	{
		if( mapFeaturesEnabled )
		{
			mineshaftGenerator.func_151539_a( null, worldObj, cubeX, cubeY, (Block[])null );
			villageGenerator.func_151539_a( null, worldObj, cubeX, cubeY, (Block[])null );
			strongholdGenerator.func_151539_a( null, worldObj, cubeX, cubeY, (Block[])null );
			scatteredFeatureGenerator.func_151539_a( null, worldObj, cubeX, cubeY, (Block[])null );
		}
	}
}
