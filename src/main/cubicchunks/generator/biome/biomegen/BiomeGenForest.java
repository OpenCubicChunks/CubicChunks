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

import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;
import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenBigMushroomCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenBirchTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenCanopyTreeCube;
import cuchaz.cubicChunks.util.Coords;

import java.util.Random;

<<<<<<< HEAD:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenForest.java
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenForest.java
=======
import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.Height;
import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenForest.java
=======
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.Height;
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 69175bb... - Refactored package structure again to remove /java/:src/main/cubicchunks/generator/biome/biomegen/BiomeGenForest.java
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BiomeGenForest extends CubeBiomeGenBase
{
	protected static final WorldGenBirchTreeCube wGenTrees1 = new WorldGenBirchTreeCube( false, true );
	protected static final WorldGenBirchTreeCube wGenTrees2 = new WorldGenBirchTreeCube( false, false );
	protected static final WorldGenCanopyTreeCube wGenCanopyTree = new WorldGenCanopyTreeCube( false );
	private static final int FOREST = 0;
	private static final int FOREST_MUTATED = 1;
	private static final int BIRCH_FOREST = 2;
	private static final int ROOFED_FOREST = 3;
	private final int variant;
	
	@SuppressWarnings( "unchecked" )
	public BiomeGenForest( int biomeID, int variant )
	{
		super( biomeID );
		this.variant = variant;
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		cfg.treesPerColumn( 10 );
		cfg.grassPerColumn( 2 );
		
		if( this.variant == FOREST_MUTATED )
		{
			cfg.treesPerColumn( 6 );
			cfg.flowersPerColumn( 100 );
			cfg.grassPerColumn( 1 );
		}
		
		this.func_76733_a( 5159473 );
		this.setTemperatureAndRainfall( 0.7F, 0.8F );
		
		if( this.variant == BIRCH_FOREST )
		{
			this.field_150609_ah = 353825;
			this.color = 3175492;
			this.setTemperatureAndRainfall( 0.6F, 0.6F );
		}
		
		if( this.variant == FOREST )
		{
			this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityWolf.class, 5, 4, 4 ) );
		}
		
		if( this.variant == ROOFED_FOREST )
		{
			// decorate() overrides tree generation for roofed forest
			cfg.treesPerColumn( DISABLE );
		}
	}
	
	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		if( variant == ROOFED_FOREST && rand.nextInt( 3 ) > 0 )
		{
			return wGenCanopyTree;
		}
		
		if( this.variant != BIRCH_FOREST && rand.nextInt( 5 ) != 0 )
		{
			return this.worldGeneratorTrees;
		}
		
		if( this.variant != ROOFED_FOREST )
		{
			return rand.nextInt( 3 ) == 0 ? this.worldGeneratorBigTree : wGenTrees2;
		}
		return wGenTrees2;
	}
	
	@Override
	public String spawnFlower( Random rand, int x, int y, int z )
	{
		if( this.variant == FOREST_MUTATED )
		{
			double var5 = MathHelper.clamp_double( ( 1.0D + field_150606_ad.func_151601_a( (double)x / 48.0D, (double)z / 48.0D ) ) / 2.0D, 0.0D, 0.9999D );
			int var7 = (int)( var5 * (double)BlockFlower.field_149859_a.length );
			
			if( var7 == 1 )
			{
				var7 = 0;
			}
			
			return BlockFlower.field_149859_a[var7];
		}
		else
		{
			return super.spawnFlower( rand, x, y, z );
		}
	}
	
	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		
		int minY = Coords.cubeToMinBlock( y ) + 8;
		int maxY = minY + 16;
		if( this.variant == ROOFED_FOREST )
		{
			for( int xGrid = 0; xGrid < 4; ++xGrid )
			{
				for( int zGrid = 0; zGrid < 4; ++zGrid )
				{
					int xAbs = Coords.cubeToMinBlock( x ) + xGrid * 4 + 1 + 8 + rand.nextInt( 3 );
					int zAbs = Coords.cubeToMinBlock( z ) + zGrid * 4 + 1 + 8 + rand.nextInt( 3 );
					int yAbs = world.getHeightValue( xAbs, zAbs );
					
					if( yAbs < minY || yAbs > maxY )
					{
						continue;
					}
					
					if( rand.nextInt( 20 ) == 0 )
					{
						WorldGenBigMushroomCube generator = new WorldGenBigMushroomCube();
						generator.generate( world, rand, xAbs, yAbs, zAbs );
					}
					else
					{
						WorldGenAbstractTreeCube wGenTree = this.checkSpawnTree( rand );
						wGenTree.setScale( 1.0D, 1.0D, 1.0D );
						
						if( wGenTree.generate( world, rand, xAbs, yAbs, zAbs ) )
						{
							wGenTree.afterGenerate( world, rand, xAbs, yAbs, zAbs );
						}
					}
				}
			}
		}
		
		int maxGen = rand.nextInt( 5 ) - 3;
		
		if( this.variant == FOREST_MUTATED )
		{
			maxGen += 2;
		}
		
		int n = 0;
		
		while( n < maxGen )
		{
			int rand1 = rand.nextInt( 10 );
			
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
				if( n2 < 5 & rand1 > 6 )
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
	 * Provides the basic grass color based on the biome temperature and
	 * rainfall
	 */
	@Override
	public int getBiomeGrassColor( int x, int y, int z )
	{
		int color = super.getBiomeGrassColor( x, y, z );
		return this.variant == ROOFED_FOREST ? ( color & 16711422 ) + 2634762 >> 1 : color;
	}
	
	@Override
	protected CubeBiomeGenBase func_150557_a( int p_150557_1_, boolean p_150557_2_ )
	{
		if( this.variant == BIRCH_FOREST )
		{
			this.field_150609_ah = 353825;
			this.color = p_150557_1_;
			
			if( p_150557_2_ )
			{
				this.field_150609_ah = ( this.field_150609_ah & 16711422 ) >> 1;
			}
			
			return this;
		}
		else
		{
			return super.func_150557_a( p_150557_1_, p_150557_2_ );
		}
	}
	
	@Override
	protected CubeBiomeGenBase createAndReturnMutated( )
	{
		if( this.biomeID == CubeBiomeGenBase.forest.biomeID )
		{
			BiomeGenForest biome = new BiomeGenForest( this.biomeID + 128, FOREST_MUTATED );
			biome.setHeightRange( new CubeBiomeGenBase.Height( this.biomeHeight, this.biomeVolatility + 0.2F ) );
			biome.setBiomeName( "Flower Forest" );
			biome.func_150557_a( 6976549, true );
			biome.func_76733_a( 8233509 );
			return biome;
		}
		else
		{
			return this.biomeID != CubeBiomeGenBase.birchForest.biomeID && this.biomeID != CubeBiomeGenBase.birchForestHills.biomeID ? new BiomeGenMutated( this.biomeID + 128, this )
			{
				@Override
				public void decorate( World var1, Random var2, int var3, int var4 )
				{
					this.biome.decorate( var1, var2, var3, var4 );
				}
			} : new BiomeGenMutated( this.biomeID + 128, this )
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
