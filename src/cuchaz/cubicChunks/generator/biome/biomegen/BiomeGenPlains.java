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
import cuchaz.cubicChunks.util.Coords;
import java.util.Random;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;

public class BiomeGenPlains extends CubeBiomeGenBase
{
	protected boolean generateAdditionalDoblePlants;

	protected BiomeGenPlains( int biomeID )
	{
		super( biomeID );
		this.setTemperatureAndRainfall( 0.8F, 0.4F );
		this.setHeightRange( PlainsRange );
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityHorse.class, 5, 2, 6 ) );
		this.decorator().treesPerChunk = -999;
		this.decorator().flowersPerChunk = 4;
		this.decorator().grassPerChunk = 10;
	}

	@Override
	public String spawnFlower( Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_ )
	{
		double var5 = field_150606_ad.func_151601_a( (double)p_150572_2_ / 200.0D, (double)p_150572_4_ / 200.0D );
		int var7;

		if( var5 < -0.8D )
		{
			var7 = p_150572_1_.nextInt( 4 );
			return BlockFlower.field_149859_a[4 + var7];
		}
		else if( p_150572_1_.nextInt( 3 ) > 0 )
		{
			var7 = p_150572_1_.nextInt( 3 );
			return var7 == 0 ? BlockFlower.field_149859_a[0] : (var7 == 1 ? BlockFlower.field_149859_a[3] : BlockFlower.field_149859_a[8]);
		}
		else
		{
			return BlockFlower.field_149858_b[0];
		}
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		double d1 = field_150606_ad.func_151601_a( (x + 8) / 200.0D, (z + 8) / 200.0D );

		if( d1 < -0.8D )
		{
			this.decorator().flowersPerChunk = 15;
			this.decorator().grassPerChunk = 5;
		}
		else
		{
			this.decorator().flowersPerChunk = 4;
			this.decorator().grassPerChunk = 10;

			worldGenDoublePlant.setType( 2 );
			gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 7, 1 );
		}

		if( this.generateAdditionalDoblePlants )
		{
			worldGenDoublePlant.setType( 0 );
			gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 10, 1 );
		}

		super.decorate( world, rand, x, y, z );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		BiomeGenPlains biome = new BiomeGenPlains( this.biomeID + 128 );
		biome.setBiomeName( "Sunflower Plains" );
		biome.generateAdditionalDoblePlants = true;
		biome.setColor( 9286496 );
		biome.field_150609_ah = 14273354;
		return biome;
	}
}
