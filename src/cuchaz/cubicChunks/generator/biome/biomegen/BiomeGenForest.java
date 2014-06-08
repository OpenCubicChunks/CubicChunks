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

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenCanopyTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenForestCube;
import java.util.Random;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;

public class BiomeGenForest extends CubeBiomeGenBase
{
	protected static final WorldGenForestCube wGenTrees1 = new WorldGenForestCube( false, true );
	protected static final WorldGenForestCube wGenTrees2 = new WorldGenForestCube( false, false );
	protected static final WorldGenCanopyTreeCube wGenConopyTree = new WorldGenCanopyTreeCube( false );
	private final int variant;

	public BiomeGenForest( int biomeID, int variant )
	{
		super( biomeID );
		this.variant = variant;
		this.decorator().treesPerChunk = 10;
		this.decorator().grassPerChunk = 2;

		if( this.variant == 1 )
		{
			this.decorator().treesPerChunk = 6;
			this.decorator().flowersPerChunk = 100;
			this.decorator().grassPerChunk = 1;
		}

		this.func_76733_a( 5159473 );
		this.setTemperatureAndRainfall( 0.7F, 0.8F );

		if( this.variant == 2 )
		{
			this.field_150609_ah = 353825;
			this.color = 3175492;
			this.setTemperatureAndRainfall( 0.6F, 0.6F );
		}

		if( this.variant == 0 )
		{
			this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityWolf.class, 5, 4, 4 ) );
		}

		if( this.variant == 3 )
		{
			this.decorator().treesPerChunk = -999;
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return (this.variant == 3 && p_150567_1_.nextInt( 3 ) > 0 ? wGenConopyTree : (this.variant != 2 && p_150567_1_.nextInt( 5 ) != 0 ? this.worldGeneratorTrees : wGenTrees2));
	}

	@Override
	public String spawnFlower( Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_ )
	{
		if( this.variant == 1 )
		{
			double var5 = MathHelper.clamp_double( (1.0D + field_150606_ad.func_151601_a( (double)p_150572_2_ / 48.0D, (double)p_150572_4_ / 48.0D )) / 2.0D, 0.0D, 0.9999D );
			int var7 = (int)(var5 * (double)BlockFlower.field_149859_a.length);

			if( var7 == 1 )
			{
				var7 = 0;
			}

			return BlockFlower.field_149859_a[var7];
		}
		else
		{
			return super.spawnFlower( p_150572_1_, p_150572_2_, p_150572_3_, p_150572_4_ );
		}
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );

		if( this.variant == 3 )
		{
			gen.generateAtSurface( new WorldGenBigMushroom(), 16, 0.05D );
			for( int i = 0; i < 16; i++ )
			{
				WorldGenAbstractTreeCube generator = this.checkSpawnTree( rand );
				generator.setScale( 1.0D, 1.0D, 1.0D );
				gen.generateAtSurface( generator, 16, 0.95D );
			}
		}

		int maxGen = rand.nextInt( 5 ) - 3;

		if( this.variant == 1 )
		{
			maxGen += 2;
		}

		int n = 0;

		while( n < maxGen )
		{
			int rand1 = rand.nextInt( 3 );

			if( rand1 == 0 )
			{
				worldGenDoublePlant.setType( 1 );
			}
			else if( rand1 == 1 )
			{
				worldGenDoublePlant.setType( 4 );
			}
			else if( rand1 == 2 )
			{
				worldGenDoublePlant.setType( 5 );
			}

			int n2 = 0;

			while( true )
			{
				if( n2 < 5 )
				{
					if( !gen.generateAtSurface( worldGenDoublePlant, 1, 1 ) )
					{
						++n2;
						continue;
					}
				}

				++n;
				break;
			}
		}

		super.decorate( world, rand, x, y, z );
	}

	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeGrassColor( int p_150558_1_, int p_150558_2_, int p_150558_3_ )
	{
		int var4 = super.getBiomeGrassColor( p_150558_1_, p_150558_2_, p_150558_3_ );
		return this.variant == 3 ? (var4 & 16711422) + 2634762 >> 1 : var4;
	}

	@Override
	protected CubeBiomeGenBase func_150557_a( int p_150557_1_, boolean p_150557_2_ )
	{
		if( this.variant == 2 )
		{
			this.field_150609_ah = 353825;
			this.color = p_150557_1_;

			if( p_150557_2_ )
			{
				this.field_150609_ah = (this.field_150609_ah & 16711422) >> 1;
			}

			return this;
		}
		else
		{
			return super.func_150557_a( p_150557_1_, p_150557_2_ );
		}
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		if( this.biomeID == CubeBiomeGenBase.forest.biomeID )
		{
			BiomeGenForest biome = new BiomeGenForest( this.biomeID + 128, 1 );
			biome.setHeightRange( new CubeBiomeGenBase.Height( this.biomeHeight, this.biomeVolatility + 0.2F ) );
			biome.setBiomeName( "Flower Forest" );
			biome.func_150557_a( 6976549, true );
			biome.func_76733_a( 8233509 );
			return biome;
		}
		else
		{
			return this.biomeID != CubeBiomeGenBase.birchForest.biomeID && this.biomeID != CubeBiomeGenBase.birchForestHills.biomeID
				? new BiomeGenMutated( this.biomeID + 128, this )
				{
					@Override
					public void decorate( World var1, Random var2, int var3, int var4 )
					{
						this.biome.decorate( var1, var2, var3, var4 );
					}
				}
				: new BiomeGenMutated( this.biomeID + 128, this )
				{
					@Override
					public WorldGenAbstractTreeCube checkSpawnTree( Random var1 )
					{
						return var1.nextBoolean() ? BiomeGenForest.wGenTrees1 : BiomeGenForest.wGenTrees2;
					}
				};
		}
	}
}
