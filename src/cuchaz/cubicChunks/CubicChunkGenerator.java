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
package cuchaz.cubicChunks;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
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

public class CubicChunkGenerator implements IChunkProvider
{
	/** RNG. */
	private Random rand;
	private NoiseGeneratorOctaves field_147431_j;
	private NoiseGeneratorOctaves field_147432_k;
	private NoiseGeneratorOctaves field_147429_l;
	private NoiseGeneratorPerlin field_147430_m;
	
	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen5;
	
	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen6;
	public NoiseGeneratorOctaves mobSpawnerNoise;
	
	/** Reference to the World object. */
	private World worldObj;
	
	/** are map structures going to be generated (e.g. strongholds) */
	private final boolean mapFeaturesEnabled;
	private WorldType field_147435_p;
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
	
	public CubicChunkGenerator( World world )
	{
		this.worldObj = world;
		this.mapFeaturesEnabled = world.getWorldInfo().isMapFeaturesEnabled();
		this.field_147435_p = world.getWorldInfo().getTerrainType();
		this.rand = new Random( world.getSeed() );
		this.field_147431_j = new NoiseGeneratorOctaves( this.rand, 16 );
		this.field_147432_k = new NoiseGeneratorOctaves( this.rand, 16 );
		this.field_147429_l = new NoiseGeneratorOctaves( this.rand, 8 );
		this.field_147430_m = new NoiseGeneratorPerlin( this.rand, 4 );
		this.noiseGen5 = new NoiseGeneratorOctaves( this.rand, 10 );
		this.noiseGen6 = new NoiseGeneratorOctaves( this.rand, 16 );
		this.mobSpawnerNoise = new NoiseGeneratorOctaves( this.rand, 8 );
		this.field_147434_q = new double[825];
		this.field_147433_r = new float[25];
		
		for( int var5 = -2; var5 <= 2; ++var5 )
		{
			for( int var6 = -2; var6 <= 2; ++var6 )
			{
				float var7 = 10.0F / MathHelper.sqrt_float( (float)( var5 * var5 + var6 * var6 ) + 0.2F );
				this.field_147433_r[var5 + 2 + ( var6 + 2 ) * 5] = var7;
			}
		}
	}
	
	public void func_147424_a( int p_147424_1_, int p_147424_2_, Block[] p_147424_3_ )
	{
		byte var4 = 63;
		this.biomesForGeneration = this.worldObj.getWorldChunkManager().getBiomesForGeneration( this.biomesForGeneration, p_147424_1_ * 4 - 2, p_147424_2_ * 4 - 2, 10, 10 );
		this.func_147423_a( p_147424_1_ * 4, 0, p_147424_2_ * 4 );
		
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
					double var16 = this.field_147434_q[var9 + var13];
					double var18 = this.field_147434_q[var10 + var13];
					double var20 = this.field_147434_q[var11 + var13];
					double var22 = this.field_147434_q[var12 + var13];
					double var24 = ( this.field_147434_q[var9 + var13 + 1] - var16 ) * var14;
					double var26 = ( this.field_147434_q[var10 + var13 + 1] - var18 ) * var14;
					double var28 = ( this.field_147434_q[var11 + var13 + 1] - var20 ) * var14;
					double var30 = ( this.field_147434_q[var12 + var13 + 1] - var22 ) * var14;
					
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
									p_147424_3_[var44 += var45] = Blocks.stone;
								}
								else if( var13 * 8 + var32 < var4 )
								{
									p_147424_3_[var44 += var45] = Blocks.water;
								}
								else
								{
									p_147424_3_[var44 += var45] = null;
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
	
	public void func_147422_a( int p_147422_1_, int p_147422_2_, Block[] p_147422_3_, byte[] p_147422_4_, BiomeGenBase[] p_147422_5_ )
	{
		double var6 = 0.03125D;
		this.stoneNoise = this.field_147430_m.func_151599_a( this.stoneNoise, (double)( p_147422_1_ * 16 ), (double)( p_147422_2_ * 16 ), 16, 16, var6 * 2.0D, var6 * 2.0D, 1.0D );
		
		for( int var8 = 0; var8 < 16; ++var8 )
		{
			for( int var9 = 0; var9 < 16; ++var9 )
			{
				BiomeGenBase var10 = p_147422_5_[var9 + var8 * 16];
				var10.func_150573_a( this.worldObj, this.rand, p_147422_3_, p_147422_4_, p_147422_1_ * 16 + var8, p_147422_2_ * 16 + var9, this.stoneNoise[var9 + var8 * 16] );
			}
		}
	}
	
	/**
	 * loads or generates the chunk at the chunk location specified
	 */
	@Override
	public Chunk loadChunk( int par1, int par2 )
	{
		return this.provideChunk( par1, par2 );
	}
	
	/**
	 * Will return back a chunk, if it doesn't exist and its not a MP client it
	 * will generates all the blocks for the specified chunk from the map seed
	 * and chunk seed
	 */
	@Override
	public Column provideChunk( int par1, int par2 )
	{
		this.rand.setSeed( (long)par1 * 341873128712L + (long)par2 * 132897987541L );
		Block[] var3 = new Block[65536];
		byte[] var4 = new byte[65536];
		this.func_147424_a( par1, par2, var3 );
		this.biomesForGeneration = this.worldObj.getWorldChunkManager().loadBlockGeneratorData( this.biomesForGeneration, par1 * 16, par2 * 16, 16, 16 );
		this.func_147422_a( par1, par2, var3, var4, this.biomesForGeneration );
		this.caveGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
		this.ravineGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
		
		if( this.mapFeaturesEnabled )
		{
			this.mineshaftGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
			this.villageGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
			this.strongholdGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
			this.scatteredFeatureGenerator.func_151539_a( this, this.worldObj, par1, par2, var3 );
		}
		
		Column var5 = new Column( this.worldObj, var3, var4, par1, par2 );
		byte[] var6 = var5.getBiomeArray();
		
		for( int var7 = 0; var7 < var6.length; ++var7 )
		{
			var6[var7] = (byte)this.biomesForGeneration[var7].biomeID;
		}
		
		var5.generateSkylightMap();
		return var5;
	}
	
	private void func_147423_a( int p_147423_1_, int p_147423_2_, int p_147423_3_ )
	{
		this.field_147426_g = this.noiseGen6.generateNoiseOctaves( this.field_147426_g, p_147423_1_, p_147423_3_, 5, 5, 200.0D, 200.0D, 0.5D );
		this.field_147427_d = this.field_147429_l.generateNoiseOctaves( this.field_147427_d, p_147423_1_, p_147423_2_, p_147423_3_, 5, 33, 5, 8.555150000000001D, 4.277575000000001D,
				8.555150000000001D );
		this.field_147428_e = this.field_147431_j.generateNoiseOctaves( this.field_147428_e, p_147423_1_, p_147423_2_, p_147423_3_, 5, 33, 5, 684.412D, 684.412D, 684.412D );
		this.field_147425_f = this.field_147432_k.generateNoiseOctaves( this.field_147425_f, p_147423_1_, p_147423_2_, p_147423_3_, 5, 33, 5, 684.412D, 684.412D, 684.412D );
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
				BiomeGenBase var22 = this.biomesForGeneration[var16 + 2 + ( var17 + 2 ) * 10];
				
				for( int var23 = -var21; var23 <= var21; ++var23 )
				{
					for( int var24 = -var21; var24 <= var21; ++var24 )
					{
						BiomeGenBase var25 = this.biomesForGeneration[var16 + var23 + 2 + ( var17 + var24 + 2 ) * 10];
						float var26 = var25.minHeight;
						float var27 = var25.maxHeight;
						
						if( this.field_147435_p == WorldType.field_151360_e && var26 > 0.0F )
						{
							var26 = 1.0F + var26 * 2.0F;
							var27 = 1.0F + var27 * 4.0F;
						}
						
						float var28 = this.field_147433_r[var23 + 2 + ( var24 + 2 ) * 5] / ( var26 + 2.0F );
						
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
				double var46 = this.field_147426_g[var13] / 8000.0D;
				
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
					
					double var34 = this.field_147428_e[var12] / 512.0D;
					double var36 = this.field_147425_f[var12] / 512.0D;
					double var38 = ( this.field_147427_d[var12] / 10.0D + 1.0D ) / 2.0D;
					double var40 = MathHelper.denormalizeClamp( var34, var36, var38 ) - var32;
					
					if( var31 > 29 )
					{
						double var42 = (double)( (float)( var31 - 29 ) / 3.0F );
						var40 = var40 * ( 1.0D - var42 ) + -10.0D * var42;
					}
					
					this.field_147434_q[var12] = var40;
					++var12;
				}
			}
		}
	}
	
	/**
	 * Checks to see if a chunk exists at x, y
	 */
	@Override
	public boolean chunkExists( int par1, int par2 )
	{
		return true;
	}
	
	/**
	 * Populates chunk with ores etc etc
	 */
	@Override
	public void populate( IChunkProvider par1IChunkProvider, int par2, int par3 )
	{
		BlockFalling.field_149832_M = true;
		int var4 = par2 * 16;
		int var5 = par3 * 16;
		BiomeGenBase var6 = this.worldObj.getBiomeGenForCoords( var4 + 16, var5 + 16 );
		this.rand.setSeed( this.worldObj.getSeed() );
		long var7 = this.rand.nextLong() / 2L * 2L + 1L;
		long var9 = this.rand.nextLong() / 2L * 2L + 1L;
		this.rand.setSeed( (long)par2 * var7 + (long)par3 * var9 ^ this.worldObj.getSeed() );
		boolean var11 = false;
		
		if( this.mapFeaturesEnabled )
		{
			this.mineshaftGenerator.generateStructuresInChunk( this.worldObj, this.rand, par2, par3 );
			var11 = this.villageGenerator.generateStructuresInChunk( this.worldObj, this.rand, par2, par3 );
			this.strongholdGenerator.generateStructuresInChunk( this.worldObj, this.rand, par2, par3 );
			this.scatteredFeatureGenerator.generateStructuresInChunk( this.worldObj, this.rand, par2, par3 );
		}
		
		int var12;
		int var13;
		int var14;
		
		if( var6 != BiomeGenBase.desert && var6 != BiomeGenBase.desertHills && !var11 && this.rand.nextInt( 4 ) == 0 )
		{
			var12 = var4 + this.rand.nextInt( 16 ) + 8;
			var13 = this.rand.nextInt( 256 );
			var14 = var5 + this.rand.nextInt( 16 ) + 8;
			( new WorldGenLakes( Blocks.water ) ).generate( this.worldObj, this.rand, var12, var13, var14 );
		}
		
		if( !var11 && this.rand.nextInt( 8 ) == 0 )
		{
			var12 = var4 + this.rand.nextInt( 16 ) + 8;
			var13 = this.rand.nextInt( this.rand.nextInt( 248 ) + 8 );
			var14 = var5 + this.rand.nextInt( 16 ) + 8;
			
			if( var13 < 63 || this.rand.nextInt( 10 ) == 0 )
			{
				( new WorldGenLakes( Blocks.lava ) ).generate( this.worldObj, this.rand, var12, var13, var14 );
			}
		}
		
		for( var12 = 0; var12 < 8; ++var12 )
		{
			var13 = var4 + this.rand.nextInt( 16 ) + 8;
			var14 = this.rand.nextInt( 256 );
			int var15 = var5 + this.rand.nextInt( 16 ) + 8;
			( new WorldGenDungeons() ).generate( this.worldObj, this.rand, var13, var14, var15 );
		}
		
		var6.decorate( this.worldObj, this.rand, var4, var5 );
		SpawnerAnimals.performWorldGenSpawning( this.worldObj, var6, var4 + 8, var5 + 8, 16, 16, this.rand );
		var4 += 8;
		var5 += 8;
		
		for( var12 = 0; var12 < 16; ++var12 )
		{
			for( var13 = 0; var13 < 16; ++var13 )
			{
				var14 = this.worldObj.getPrecipitationHeight( var4 + var12, var5 + var13 );
				
				if( this.worldObj.isBlockFreezable( var12 + var4, var14 - 1, var13 + var5 ) )
				{
					this.worldObj.setBlock( var12 + var4, var14 - 1, var13 + var5, Blocks.ice, 0, 2 );
				}
				
				if( this.worldObj.func_147478_e( var12 + var4, var14, var13 + var5, true ) )
				{
					this.worldObj.setBlock( var12 + var4, var14, var13 + var5, Blocks.snow_layer, 0, 2 );
				}
			}
		}
		
		BlockFalling.field_149832_M = false;
	}
	
	/**
	 * Two modes of operation: if passed true, save all Chunks in one go. If
	 * passed false, save up to two chunks. Return true if all chunks have been
	 * saved.
	 */
	@Override
	public boolean saveChunks( boolean par1, IProgressUpdate par2IProgressUpdate )
	{
		return true;
	}
	
	/**
	 * Save extra data not associated with any Chunk. Not saved during autosave,
	 * only during world unload. Currently unimplemented.
	 */
	@Override
	public void saveExtraData( )
	{
	}
	
	/**
	 * Unloads chunks that are marked to be unloaded. This is not guaranteed to
	 * unload every such chunk.
	 */
	@Override
	public boolean unloadQueuedChunks( )
	{
		return false;
	}
	
	/**
	 * Returns if the IChunkProvider supports saving.
	 */
	@Override
	public boolean canSave( )
	{
		return true;
	}
	
	/**
	 * Converts the instance data to a readable string.
	 */
	@Override
	public String makeString( )
	{
		return "RandomLevelSource";
	}
	
	/**
	 * Returns a list of creatures of the specified type that can spawn at the
	 * given location.
	 */
	@Override
	public List getPossibleCreatures( EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4 )
	{
		BiomeGenBase var5 = this.worldObj.getBiomeGenForCoords( par2, par4 );
		return par1EnumCreatureType == EnumCreatureType.monster && this.scatteredFeatureGenerator.func_143030_a( par2, par3, par4 ) ? this.scatteredFeatureGenerator.getScatteredFeatureSpawnList()
				: var5.getSpawnableList( par1EnumCreatureType );
	}
	
	@Override
	public ChunkPosition func_147416_a( World p_147416_1_, String p_147416_2_, int p_147416_3_, int p_147416_4_, int p_147416_5_ )
	{
		return "Stronghold".equals( p_147416_2_ ) && this.strongholdGenerator != null ? this.strongholdGenerator.func_151545_a( p_147416_1_, p_147416_3_, p_147416_4_, p_147416_5_ ) : null;
	}
	
	@Override
	public int getLoadedChunkCount( )
	{
		return 0;
	}
	
	@Override
	public void recreateStructures( int par1, int par2 )
	{
		if( this.mapFeaturesEnabled )
		{
			this.mineshaftGenerator.func_151539_a( this, this.worldObj, par1, par2, (Block[])null );
			this.villageGenerator.func_151539_a( this, this.worldObj, par1, par2, (Block[])null );
			this.strongholdGenerator.func_151539_a( this, this.worldObj, par1, par2, (Block[])null );
			this.scatteredFeatureGenerator.func_151539_a( this, this.worldObj, par1, par2, (Block[])null );
		}
	}
}
