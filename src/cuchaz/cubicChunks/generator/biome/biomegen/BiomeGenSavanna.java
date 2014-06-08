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
import cuchaz.cubicChunks.generator.populator.generators.WorldGenSavannaTreeCube;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class BiomeGenSavanna extends CubeBiomeGenBase
{
	private static final WorldGenSavannaTreeCube savannaTreeGen = new WorldGenSavannaTreeCube( false );

	protected BiomeGenSavanna( int id )
	{
		super( id );
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityHorse.class, 1, 2, 6 ) );
		this.decorator().treesPerChunk = 1;
		this.decorator().flowersPerChunk = 4;
		this.decorator().grassPerChunk = 20;
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		return rand.nextInt( 5 ) > 0 ? savannaTreeGen : this.worldGeneratorTrees;
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z );
		
		worldGenDoublePlant.setType(2 );
		gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 7, 1);

		super.decorate( world, rand, x, y, z );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		BiomeGenSavanna.Mutated biome = new BiomeGenSavanna.Mutated( this.biomeID + 128, this );
		biome.temperature = (this.temperature + 1.0F) * 0.5F;
		biome.biomeHeight = this.biomeHeight * 0.5F + 0.3F;
		biome.biomeVolatility = this.biomeVolatility * 0.5F + 1.2F;
		return biome;
	}

	public static class Mutated extends BiomeGenMutated
	{
		public Mutated( int p_i45382_1_, CubeBiomeGenBase p_i45382_2_ )
		{
			super( p_i45382_1_, p_i45382_2_ );
			this.decorator().treesPerChunk = 2;
			this.decorator().flowersPerChunk = 2;
			this.decorator().grassPerChunk = 5;
		}

		@Override
		public void modifyBlocks_pre( World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double val )
		{
			this.topBlock = Blocks.grass;
			this.field_150604_aj = 0;
			this.fillerBlock = Blocks.dirt;

			if( val > 1.75D )
			{
				this.topBlock = Blocks.stone;
				this.fillerBlock = Blocks.stone;
			}
			else if( val > -0.5D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 1;
			}

			this.modifyBlocks( world, rand, cube, xAbs, yAbs, zAbs, val );
		}

		@Override
		public void decorate( World world, Random rand, int x, int y, int z )
		{
			this.decorator().decorate( world, rand, this, x, y, z );
		}
	}
}
