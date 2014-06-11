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

import cuchaz.cubicChunks.CubeWorldProvider;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;
import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenDeadBushCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenFlowersCube;
import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import cuchaz.cubicChunks.util.Coords;
import java.util.Random;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenCactus;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenSand;
import net.minecraft.world.gen.feature.WorldGenWaterlily;
import net.minecraft.world.gen.feature.WorldGenerator;

public class CubeBiomeDecorator extends BiomeDecorator
{
	private WorldGenFlowersCube flowerGen;

	protected DecoratorHelper gen;

	private DecoratorConfig cfg;

	public CubeBiomeDecorator()
	{
		this.cfg = new DecoratorConfig();
		this.sandGen = new WorldGenSand( Blocks.sand, 7 );
		this.gravelAsSandGen = new WorldGenSand( Blocks.gravel, 6 );
		this.dirtGen = new WorldGenMinable( Blocks.dirt, 32 );
		this.gravelGen = new WorldGenMinable( Blocks.gravel, 32 );
		this.coalGen = new WorldGenMinable( Blocks.coal_ore, 16 );
		this.ironGen = new WorldGenMinable( Blocks.iron_ore, 8 );
		this.goldGen = new WorldGenMinable( Blocks.gold_ore, 8 );
		this.redstoneGen = new WorldGenMinable( Blocks.redstone_ore, 7 );
		this.diamondGen = new WorldGenMinable( Blocks.diamond_ore, 7 );
		this.lapisGen = new WorldGenMinable( Blocks.lapis_ore, 6 );
		this.flowerGen = new WorldGenFlowersCube( Blocks.yellow_flower );
		this.mushroomBrownGen = new WorldGenFlowersCube( Blocks.brown_mushroom );
		this.mushroomRedGen = new WorldGenFlowersCube( Blocks.red_mushroom );
		this.bigMushroomGen = new WorldGenBigMushroom();
		this.reedGen = new WorldGenReed();
		this.cactusGen = new WorldGenCactus();
		this.waterlilyGen = new WorldGenWaterlily();
		this.cfg.flowersPerColumn( 2 );
		this.cfg.grassPerColumn( 1 );
		this.cfg.gravelPerColumn( 1 );
		this.cfg.sandPerColumn( 3 );
		this.cfg.clayPerColumn( 1 );
		this.cfg.generateLakes( true );
	}

	protected DecoratorConfig decoratorConfig()
	{
		return this.cfg;
	}

	public void func_150512_a( World world, Random rand, CubeBiomeGenBase biome, int cubeX, int cubeZ )
	{
		throw new UnsupportedOperationException( "Can't decorate Columns!" );
	}

	public void decorate( World world, Random rand, CubeBiomeGenBase biome, int cubeX, int cubeY, int cubeZ )
	{
		if( this.cfg == null )
		{
			throw new IllegalStateException( "Decorator confiug is null!" );
		}

		if( this.gen != null )
		{
			//In case if we generate something really high and poulate other cubes...
			if( gen.chunk_X == cubeX && gen.chunk_Z == cubeZ )
			{
				//do not populate this cube. We are already generatinbg something in this cube
				return;
			}
			throw new RuntimeException( "Already decorating!!" );
		}
		else
		{
			this.gen = new DecoratorHelper( world, rand, cubeX, cubeY, cubeZ );
			this.randomGenerator = rand;
			this.decorate_do( biome );
			this.gen = null;
		}
	}

	protected void decorate_do( CubeBiomeGenBase biome )
	{
		this.generateOres();

		gen.generateAtSurface( this.sandGen, cfg.sandPerColumn(), 1 );
		gen.generateAtSurface( this.clayGen, cfg.clayPerColumn(), 1 );
		gen.generateAtSurface( this.gravelAsSandGen, cfg.gravelPerColumn(), 1 );

		int trees = this.randomGenerator.nextInt( 10 ) == 0 ? cfg.treesPerColumn() + 1 : cfg.treesPerColumn();

		for( int i = 0; i < trees; ++i )
		{
			WorldGenAbstractTreeCube wGenTree = biome.checkSpawnTree( this.randomGenerator );
			wGenTree.setScale( 1.0D, 1.0D, 1.0D );
			gen.generateAtSurface( wGenTree, 1, 1 );
		}

		gen.generateAtSurface( this.bigMushroomGen, cfg.bigMushroomsPerColumn(), 1 );

		gen.generateFlowers( flowerGen, biome, cfg.flowersPerColumn(), 1 );

		gen.generateAtRand2xHeight1( biome.getRandomWorldGenForGrass( this.randomGenerator ), cfg.grassPerColumn(), 1 );
		gen.generateAtRand2xHeight1( new WorldGenDeadBushCube( Blocks.deadbush ), cfg.deadBushPerColumn(), 1 );

		gen.generateAtRand2xHeight2( this.waterlilyGen, cfg.waterlilyPerColumn(), 1 );

		gen.generateAtSurface( this.mushroomBrownGen, cfg.mushroomsPerColumn(), 0.25D );

		gen.generateAtRand2xHeight3( this.mushroomRedGen, cfg.mushroomsPerColumn() + 1, 0.125D );
		gen.generateAtRand2xHeight3( this.mushroomBrownGen, 1, 0.25D );
		gen.generateAtRand2xHeight3( this.reedGen, cfg.reedsPerColumn() < 0 ? 10 : cfg.reedsPerColumn() + 10, 1 );
		gen.generateAtRand2xHeight3( new WorldGenPumpkin(), 1, 1.0D / 32.0D );
		gen.generateAtRand2xHeight3( this.cactusGen, cfg.cactiPerColumn(), 1 );

		if( this.generateLakes )
		{
			gen.generateWater();
			gen.generateLava();
		}
	}

	/**
	 * Generates ores in the current chunk
	 */
	@Override
	protected void generateOres()
	{
		//Base parbability. In vanilla ores are generated in the whole comumn at once, so probability togenerate them in one cube is 1/16.
		double probability = 1D / 16D;
		gen.genberateAtRandomHeight( 20, probability, this.dirtGen );//0-256
		//Gravel generation disabled because of incredibly slow world saving.
		//this.generateAtRandomHeight( 10, probability, this.gravelGen, maxTerrainY );//0-256
		//generate only in range 0-128. Doubled probability
		gen.generateAtRandomHeight( 20, probability * 2, this.coalGen, 1.0D );//0-128
		gen.generateAtRandomHeight( 20, probability * 4, this.ironGen, 0.0D );//0-64//only below sea level
		gen.generateAtRandomHeight( 2, probability * 8, this.goldGen, -0.5D );//0-32 //only below 1/4 of max height
		gen.generateAtRandomHeight( 8, probability * 16, this.redstoneGen, -0.75D );//0-16
		gen.generateAtRandomHeight( 1, probability * 16, this.diamondGen, -0.75D );//0-16
		gen.generateAtRandomHeight( 1, probability * 8, this.lapisGen, -0.5D );//0-32
	}

	protected class DecoratorConfig
	{
		protected static final int DISABLE = -999;

		protected final DecoratorConfig generateLakes( boolean value )
		{
			CubeBiomeDecorator.this.generateLakes = value;
			return this;
		}

		protected final DecoratorConfig waterlilyPerColumn( int value )
		{
			CubeBiomeDecorator.this.waterlilyPerChunk = value;
			return this;
		}

		protected final DecoratorConfig treesPerColumn( int value )
		{
			CubeBiomeDecorator.this.treesPerChunk = value;
			return this;
		}

		protected final DecoratorConfig flowersPerColumn( int value )
		{
			CubeBiomeDecorator.this.flowersPerChunk = value;
			return this;
		}

		protected final DecoratorConfig grassPerColumn( int value )
		{
			CubeBiomeDecorator.this.grassPerChunk = value;
			return this;
		}

		protected final DecoratorConfig deadBushPerColumn( int value )
		{
			CubeBiomeDecorator.this.deadBushPerChunk = value;
			return this;
		}

		protected final DecoratorConfig mushroomsPerColumn( int value )
		{
			CubeBiomeDecorator.this.mushroomsPerChunk = value;
			return this;
		}

		protected final DecoratorConfig reedsPerColumn( int value )
		{
			CubeBiomeDecorator.this.reedsPerChunk = value;
			return this;
		}

		protected final DecoratorConfig cactiPerColumn( int value )
		{
			CubeBiomeDecorator.this.cactiPerChunk = value;
			return this;
		}

		protected final DecoratorConfig sandPerColumn( int value )
		{
			//the actual sane per chunk is sandPerChunk2
			CubeBiomeDecorator.this.sandPerChunk2 = value;
			return this;
		}

		protected final DecoratorConfig gravelPerColumn( int value )
		{
			//Who invented names for these variables!?
			CubeBiomeDecorator.this.sandPerChunk = value;
			return this;
		}

		protected final DecoratorConfig clayPerColumn( int value )
		{
			CubeBiomeDecorator.this.clayPerChunk = value;
			return this;
		}

		protected final DecoratorConfig bigMushroomsPerColumn( int value )
		{
			CubeBiomeDecorator.this.bigMushroomsPerChunk = value;
			return this;
		}

		protected final boolean generateLakes()
		{
			return CubeBiomeDecorator.this.generateLakes;
		}

		protected final int waterlilyPerColumn()
		{
			return CubeBiomeDecorator.this.waterlilyPerChunk;
		}

		protected final int treesPerColumn()
		{
			return CubeBiomeDecorator.this.treesPerChunk;
		}

		protected final int flowersPerColumn()
		{
			return CubeBiomeDecorator.this.flowersPerChunk;
		}

		protected final int grassPerColumn()
		{
			return CubeBiomeDecorator.this.grassPerChunk;
		}

		protected final int deadBushPerColumn()
		{
			return CubeBiomeDecorator.this.deadBushPerChunk;
		}

		protected final int mushroomsPerColumn()
		{
			return CubeBiomeDecorator.this.mushroomsPerChunk;
		}

		protected final int reedsPerColumn()
		{
			return CubeBiomeDecorator.this.reedsPerChunk;
		}

		protected final int cactiPerColumn()
		{
			return CubeBiomeDecorator.this.cactiPerChunk;
		}

		protected final int sandPerColumn()
		{
			return CubeBiomeDecorator.this.sandPerChunk2;
		}

		protected final int gravelPerColumn()
		{
			return CubeBiomeDecorator.this.sandPerChunk;
		}

		protected final int clayPerColumn()
		{
			return CubeBiomeDecorator.this.clayPerChunk;
		}

		protected final int bigMushroomsPerColumn()
		{
			return CubeBiomeDecorator.this.bigMushroomsPerChunk;
		}
	}
}
