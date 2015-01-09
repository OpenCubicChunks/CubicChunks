/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package cubicchunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;

import java.util.Random;

<<<<<<< HEAD:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenPlains.java
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenPlains.java
=======
import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenPlains.java
=======
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 69175bb... - Refactored package structure again to remove /java/:src/main/cubicchunks/generator/biome/biomegen/BiomeGenPlains.java
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;

public class BiomeGenPlains extends CubeBiomeGenBase
{
	protected boolean generateAdditionalDoblePlants;

	@SuppressWarnings("unchecked")
	protected BiomeGenPlains( int biomeID )
	{
		super( biomeID );
		this.setTemperatureAndRainfall( 0.8F, 0.4F );
		this.setHeightRange( PlainsRange );
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityHorse.class, 5, 2, 6 ) );

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn( -999 );
		cfg.flowersPerColumn( 4 );
		cfg.grassPerColumn( 10 );
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

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		
		if( d1 < -0.8D )
		{
			cfg.flowersPerColumn( 15);
			cfg.grassPerColumn( 5);
		}
		else
		{
			cfg.flowersPerColumn( 4);
			cfg.grassPerColumn( 10);

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
