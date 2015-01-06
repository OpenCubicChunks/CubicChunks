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
package cubicchunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenSavannaTreeCube;
import cuchaz.cubicChunks.world.Cube;

import java.util.Random;

import cubicchunks.world.Cube;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenSavanna.java
=======
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenSavanna.java

public class BiomeGenSavanna extends CubeBiomeGenBase
{
	private static final WorldGenSavannaTreeCube savannaTreeGen = new WorldGenSavannaTreeCube( false );

	@SuppressWarnings("unchecked")
	protected BiomeGenSavanna( int id )
	{
		super( id );
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityHorse.class, 1, 2, 6 ) );
		
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		
		cfg.treesPerColumn( 1);
		cfg.flowersPerColumn( 4);
		cfg.grassPerColumn( 20);
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		return rand.nextInt( 5 ) > 0 ? savannaTreeGen : this.worldGeneratorTrees;
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );

		worldGenDoublePlant.setType( 2 );
		gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 7, 1 );

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
		public Mutated( int biomeID, CubeBiomeGenBase newBiome )
		{
			super( biomeID, newBiome );
			
			CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
			
			cfg.treesPerColumn( 2);
			cfg.flowersPerColumn( 2);
			cfg.grassPerColumn( 5);
		}

		@Override
		public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
		{
			
			this.topBlock = Blocks.grass;
			this.field_150604_aj = 0;
			this.fillerBlock = Blocks.dirt;

			if( depthNoiseValue > 1.75D )
			{
				this.topBlock = Blocks.stone;
				this.fillerBlock = Blocks.stone;
			}
			else if( depthNoiseValue > -0.5D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 1;
			}

			super.replaceBlocks( world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, depthNoiseValue );
		}

		@Override
		public void decorate( World world, Random rand, int x, int y, int z )
		{
			this.decorator().decorate( world, rand, this, x, y, z );
		}
	}
}
