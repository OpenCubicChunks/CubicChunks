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

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import cuchaz.cubicChunks.util.HeightHelper;
import cuchaz.cubicChunks.world.Cube;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

public class BiomeGenMesa extends CubeBiomeGenBase
{
	private byte[] colorArray;
	private long lastWorldSeed;
	private NoiseGeneratorPerlin noise1;
	private NoiseGeneratorPerlin noise2;
	private NoiseGeneratorPerlin colorGenNoise;
	private final boolean isMesaMutated;
	private final boolean isForest;

	public BiomeGenMesa( int id, boolean isMesaMutated, boolean isForest )
	{
		super( id );
		this.isMesaMutated = isMesaMutated;
		this.isForest = isForest;
		this.setDisableRain();
		this.setTemperatureAndRainfall( 2.0F, 0.0F );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.field_150604_aj = 1;
		this.fillerBlock = Blocks.stained_hardened_clay;
		this.decorator().treesPerChunk = -999;
		this.decorator().deadBushPerChunk = 20;
		this.decorator().reedsPerChunk = 3;
		this.decorator().cactiPerChunk = 5;
		this.decorator().flowersPerChunk = 0;
		this.spawnableCreatureList.clear();

		if( isForest )
		{
			this.decorator().treesPerChunk = 5;
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		return this.worldGeneratorTrees;
	}

	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeFoliageColor( int p_150571_1_, int p_150571_2_, int p_150571_3_ )
	{
		return 10387789;
	}

	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeGrassColor( int p_150558_1_, int p_150558_2_, int p_150558_3_ )
	{
		return 9470285;
	}

	@Override
	public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		if( this.colorArray == null || this.lastWorldSeed != world.getSeed() )
		{
			this.generateColorArray( world.getSeed() );
		}

		if( this.noise1 == null || this.noise2 == null || this.lastWorldSeed != world.getSeed() )
		{
			Random var9 = new Random( this.lastWorldSeed );
			this.noise1 = new NoiseGeneratorPerlin( var9, 4 );
			this.noise2 = new NoiseGeneratorPerlin( var9, 1 );
		}

		this.lastWorldSeed = world.getSeed();
		double fillStoneY = 0.0D;

		if( this.isMesaMutated )
		{
			int xNoise = (xAbs & -16) + (zAbs & 15);
			int zNoise = (zAbs & -16) + (xAbs & 15);
			double noiseValue = Math.min( Math.abs( depthNoiseValue ), this.noise1.func_151601_a( (double)xNoise * 0.25D, (double)zNoise * 0.25D ) );

			if( noiseValue > 0.0D )
			{
				double scale = 0.001953125D;
				double noiseValue2 = Math.abs( this.noise2.func_151601_a( (double)xNoise * scale, (double)zNoise * scale ) );
				fillStoneY = noiseValue * noiseValue * 2.5D;
				double max = Math.ceil( noiseValue2 * 50.0D ) + 14.0D;

				if( fillStoneY > max )
				{
					fillStoneY = max;
				}
				//sea level is now 64 blocks below vanilla sea level.
				//fillStoneY += 64.0D;
			}
		}

		fillStoneY = HeightHelper.getScaledHeight_Double( fillStoneY );

		Block topBlock = Blocks.stained_hardened_clay;
		Block fillerBlock = this.fillerBlock;

		//How many biome blocks left to set in column? Initially -1
		int numBlocksToChange = -1;

		//Biome blocks depth in current block column. 0 for negative values.
		int depth = (int)(depthNoiseValue / 3.0D + 3.0D + rand.nextDouble() * 0.25D);

		boolean noTopBlock = Math.cos( depthNoiseValue / 3.0D * Math.PI ) > 0.0D;

		boolean b1 = false;

		for( int yAbs = top; yAbs >= bottom; --yAbs )
		{
			boolean canSetBlock = yAbs <= alterationTop;

			//Current block
			Block block = replaceBlocks_getBlock( cube, above, xAbs, yAbs, zAbs );

			if( (block == null || block.getMaterial() == Material.air) && yAbs < (int)fillStoneY )
			{
				replaceBlocks_setBlock( Blocks.stone, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
				block = Blocks.stone;
			}

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

			byte metadata;

			//If we are 1 block below air...
			if( numBlocksToChange == -1 )
			{
				b1 = false;
				//If depth is <= 0 - only stone
				if( depth <= 0 )
				{
					topBlock = null;
					fillerBlock = Blocks.stone;
				}
				//If we are above or at 4 block under water and at or below one block above water
				else if( yAbs >= seaLevel - 4 && yAbs <= seaLevel + 1 )
				{
					topBlock = this.topBlock;
					fillerBlock = this.fillerBlock;
				}

				//If top block is air and we are below sea level use water instead
				if( yAbs < seaLevel && (topBlock == null || topBlock.getMaterial() == Material.air) )
				{
					topBlock = Blocks.water;
					metadata = 0;
				}

				//Set num blocks to change to current depth.
				int yAbsVanilla = HeightHelper.getVanillaHeight( yAbs );
				//depth can't be higher 16 :(
				//but to be sure that everything works don't go above 15
				numBlocksToChange = Math.min( 15, depth + Math.max( 0, yAbsVanilla - 63 ) );

				if( yAbs >= seaLevel - 1 )
				{
					if( isForest && yAbs > HeightHelper.getScaledHeight( 86 ) + depth * 2 )
					{
						if( noTopBlock )
						{
							replaceBlocks_setBlock( Blocks.dirt, 1, cube, xAbs, yAbs, zAbs, canSetBlock );
						}
						else
						{
							replaceBlocks_setBlock( Blocks.grass, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
						}
					}
					else if( yAbs > HeightHelper.getScaledHeight( 66 ) + depth )
					{
						metadata = 16;
						if( yAbs >= seaLevel + 1 && yAbs <= NewTerrainProcessor.maxElev )
						{
							if( !noTopBlock )
							{
								metadata = this.getRandomMetadata( xAbs, yAbs, zAbs );
							}
						}
						else
						{
							metadata = 1;
						}

						if( metadata != 16 )
						{
							replaceBlocks_setBlock( Blocks.stained_hardened_clay, metadata, cube, xAbs, yAbs, zAbs, canSetBlock );
						}
						else
						{
							replaceBlocks_setBlock( Blocks.hardened_clay, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
						}
					}
					else
					{
						replaceBlocks_setBlock( topBlock, this.field_150604_aj, cube, xAbs, yAbs, zAbs, canSetBlock );
						b1 = true;

					}
				}
				else
				{
					replaceBlocks_setBlock( fillerBlock, fillerBlock == Blocks.stained_hardened_clay ? 1 : 0, cube, xAbs, yAbs, zAbs, canSetBlock );
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
			if( b1 )
			{
				replaceBlocks_setBlock( Blocks.stained_hardened_clay, 1, cube, xAbs, yAbs, zAbs, canSetBlock );
			}
			else
			{
				metadata = this.getRandomMetadata( xAbs, yAbs, zAbs );
				if( metadata != 16 )
				{
					replaceBlocks_setBlock( Blocks.stained_hardened_clay, metadata, cube, xAbs, yAbs, zAbs, canSetBlock );
				}
				else
				{
					replaceBlocks_setBlock( Blocks.hardened_clay, 0, cube, xAbs, yAbs, zAbs, canSetBlock );
				}
			}
		}
	}

	public void modifyBlocks_pre( World world, Random rand, Block[] blocks, byte[] meta, int xAbs, int yAbs, int zAbs, double val )
	{
		if( this.colorArray == null || this.lastWorldSeed != world.getSeed() )
		{
			this.generateColorArray( world.getSeed() );
		}

		if( this.noise1 == null || this.noise2 == null || this.lastWorldSeed != world.getSeed() )
		{
			Random var9 = new Random( this.lastWorldSeed );
			this.noise1 = new NoiseGeneratorPerlin( var9, 4 );
			this.noise2 = new NoiseGeneratorPerlin( var9, 1 );
		}

		this.lastWorldSeed = world.getSeed();
		double fillStoneY = 0.0D;

		if( this.isMesaMutated )
		{
			int xNoise = (xAbs & -16) + (zAbs & 15);
			int zNoise = (zAbs & -16) + (xAbs & 15);
			double noiseValue = Math.min( Math.abs( val ), this.noise1.func_151601_a( (double)xNoise * 0.25D, (double)zNoise * 0.25D ) );

			if( noiseValue > 0.0D )
			{
				double scale = 0.001953125D;
				double noiseValue2 = Math.abs( this.noise2.func_151601_a( (double)xNoise * scale, (double)zNoise * scale ) );
				fillStoneY = noiseValue * noiseValue * 2.5D;
				double max = Math.ceil( noiseValue2 * 50.0D ) + 14.0D;

				if( fillStoneY > max )
				{
					fillStoneY = max;
				}
				//sea level is now 64 blocks below vanilla sea level.
				//fillStoneY += 64.0D;
			}
		}

		//maxElev for vanilla would be 64 (64 blocks above sea level)
		double terrainHeightScale = NewTerrainProcessor.maxElev / 64D;
		fillStoneY *= terrainHeightScale;

		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;
		boolean var26 = true;
		Block var14 = Blocks.stained_hardened_clay;
		Block var27 = this.fillerBlock;
		int var16 = (int)(val / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
		boolean var28 = Math.cos( val / 3.0D * Math.PI ) > 0.0D;
		int var18 = -1;
		boolean var29 = false;
		int var20 = blocks.length / 256;

		for( int y = 16; y >= 0; --y )
		{
			int loc = (zRel * 16 + xRel) * 16 + y;

			if( (blocks[loc] == null || blocks[loc].getMaterial() == Material.air) && yAbs < (int)fillStoneY )
			{
				blocks[loc] = Blocks.stone;
			}

			if( yAbs <= 0 + rand.nextInt( 5 ) )
			{
				blocks[loc] = Blocks.bedrock;
			}
			else
			{
				Block block = blocks[loc];

				if( block != null && block.getMaterial() != Material.air )
				{
					if( block == Blocks.stone )
					{
						byte var24;

						if( var18 == -1 )
						{
							var29 = false;

							if( var16 <= 0 )
							{
								var14 = null;
								var27 = Blocks.stone;
							}
							else if( yAbs >= 59 && y <= 64 )
							{
								var14 = Blocks.stained_hardened_clay;
								var27 = this.fillerBlock;
							}

							if( yAbs < 63 && (var14 == null || var14.getMaterial() == Material.air) )
							{
								var14 = Blocks.water;
							}

							var18 = var16 + Math.max( 0, y - 63 );

							if( yAbs >= 62 )
							{
								if( this.isForest && y > 86 + var16 * 2 )
								{
									if( var28 )
									{
										blocks[loc] = Blocks.dirt;
										meta[loc] = 1;
									}
									else
									{
										blocks[loc] = Blocks.grass;
									}
								}
								else if( yAbs > 66 + var16 )
								{
									var24 = 16;

									if( yAbs >= 64 && yAbs <= 127 )
									{
										if( !var28 )
										{
											var24 = this.getRandomMetadata( xAbs, y, zAbs );
										}
									}
									else
									{
										var24 = 1;
									}

									if( var24 < 16 )
									{
										blocks[loc] = Blocks.stained_hardened_clay;
										meta[loc] = (byte)var24;
									}
									else
									{
										blocks[loc] = Blocks.hardened_clay;
									}
								}
								else
								{
									blocks[loc] = this.topBlock;
									meta[loc] = (byte)this.field_150604_aj;
									var29 = true;
								}
							}
							else
							{
								blocks[loc] = var27;

								if( var27 == Blocks.stained_hardened_clay )
								{
									meta[loc] = 1;
								}
							}
						}
						else if( var18 > 0 )
						{
							--var18;

							if( var29 )
							{
								blocks[loc] = Blocks.stained_hardened_clay;
								meta[loc] = 1;
							}
							else
							{
								var24 = this.getRandomMetadata( xAbs, y, zAbs );

								if( var24 < 16 )
								{
									blocks[loc] = Blocks.stained_hardened_clay;
									meta[loc] = var24;
								}
								else
								{
									blocks[loc] = Blocks.hardened_clay;
								}
							}
						}
					}
				}
				else
				{
					var18 = -1;
				}
			}
		}
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		boolean notPlateau = this.biomeID == CubeBiomeGenBase.mesa.biomeID;
		BiomeGenMesa newBiome = new BiomeGenMesa( this.biomeID + 128, notPlateau, this.isForest );

		if( !notPlateau )
		{
			newBiome.setHeightRange( hillsRange );
			newBiome.setBiomeName( this.biomeName + " M" );
		}
		else
		{
			newBiome.setBiomeName( this.biomeName + " (Bryce)" );
		}

		newBiome.func_150557_a( this.color, true );
		return newBiome;
	}

	private void generateColorArray( long seed )
	{
		this.colorArray = new byte[64];

		//fill array with 16 (unspecified color)
		Arrays.fill( this.colorArray, (byte)16 );

		Random rand = new Random( seed );
		this.colorGenNoise = new NoiseGeneratorPerlin( rand, 1 );

		//fill random places in array with 1 (orange)
		for( int i = 0; i < 64; ++i )
		{
			i += rand.nextInt( 5 ) + 1;

			if( i < 64 )
			{
				this.colorArray[i] = 1;
			}
		}

		//add some yellow color
		int numYellowGenRanges = rand.nextInt( 4 ) + 2;
		for( int i = 0; i < numYellowGenRanges; ++i )
		{
			int rangeSize = rand.nextInt( 3 ) + 1;
			int startIndex = rand.nextInt( 64 );

			for( int j = 0; startIndex + j < 64 && j < rangeSize; ++j )
			{
				this.colorArray[startIndex + j] = 4;
			}
		}

		//add some brown color
		int numBrownGenRanges = rand.nextInt( 4 ) + 2;

		for( int i = 0; i < numBrownGenRanges; ++i )
		{
			int rangeSize = rand.nextInt( 3 ) + 2;
			int startIndex = rand.nextInt( 64 );

			for( int j = 0; startIndex + j < 64 && j < rangeSize; ++j )
			{
				this.colorArray[startIndex + j] = 12;
			}
		}

		//and red
		int numRedGenRanges = rand.nextInt( 4 ) + 2;

		for( int i = 0; i < numRedGenRanges; ++i )
		{
			int rangeSize = rand.nextInt( 3 ) + 1;
			int startIndex = rand.nextInt( 64 );

			for( int j = 0; startIndex + j < 64 && j < rangeSize; ++j )
			{
				this.colorArray[startIndex + j] = 14;
			}
		}

		//light gray
		int numGenLighrGray = rand.nextInt( 3 ) + 3;
		int rangeStart = 0;

		for( int i = 0; i < numGenLighrGray; ++i )
		{
			byte layers = 1;
			rangeStart += rand.nextInt( 16 ) + 4;

			for( int j = 0; rangeStart + j < 64 && j < layers; ++j )
			{
				this.colorArray[rangeStart + j] = 0;

				if( rangeStart + j > 1 && rand.nextBoolean() )
				{
					this.colorArray[rangeStart + j - 1] = 8;
				}

				if( rangeStart + j < 63 && rand.nextBoolean() )
				{
					this.colorArray[rangeStart + j + 1] = 8;
				}
			}
		}
	}

	private byte getRandomMetadata( int x, int y, int z )
	{
		//generate random y-shift from perlin noise
		int randomShift = (int)Math.round( this.colorGenNoise.func_151601_a( x * 1.0D / 512.0D, z * 1.0D / 512.0D ) * 2.0D );

		//colors loop if  we are above 64
		return this.colorArray[(y + randomShift + 64) % 64];
	}
}
