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
import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import cuchaz.cubicChunks.util.CubeCoordinate;
import java.util.Random;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenCactus;
import net.minecraft.world.gen.feature.WorldGenClay;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenSand;
import net.minecraft.world.gen.feature.WorldGenWaterlily;
import net.minecraft.world.gen.feature.WorldGenerator;

public class CubeBiomeDecorator extends BiomeDecorator
{
	/** Amount of waterlilys per chunk. */
	protected int waterlilyPerChunk;

	/**
	 * The number of trees to attempt to generate per chunk. Up to 10 in forests, none in deserts.
	 */
	protected int treesPerChunk;

	/**
	 * The number of yellow flower patches to generate per chunk. The game generates much less than this number, since
	 * it attempts to generate them at a random altitude.
	 */
	protected int flowersPerChunk;

	/** The amount of tall grass to generate per chunk. */
	protected int grassPerChunk;

	/**
	 * The number of dead bushes to generate per chunk. Used in deserts and swamps.
	 */
	protected int deadBushPerChunk;

	/**
	 * The number of extra mushroom patches per chunk. It generates 1/4 this number in brown mushroom patches, and 1/8
	 * this number in red mushroom patches. These mushrooms go beyond the default base number of mushrooms.
	 */
	protected int mushroomsPerChunk;

	/**
	 * The number of reeds to generate per chunk. Reeds won't generate if the randomly selected placement is unsuitable.
	 */
	protected int reedsPerChunk;

	/**
	 * The number of cactus plants to generate per chunk. Cacti only work on sand.
	 */
	protected int cactiPerChunk;

	/**
	 * The number of sand patches to generate per chunk. Sand patches only generate when part of it is underwater.
	 */
	protected int sandPerChunk;

	/**
	 * The number of sand patches to generate per chunk. Sand patches only generate when part of it is underwater. There
	 * appear to be two separate fields for this.
	 */
	protected int sandPerChunk2;

	/**
	 * The number of clay patches to generate per chunk. Only generates when part of it is underwater.
	 */
	protected int clayPerChunk;

	/** Amount of big mushrooms per chunk */
	protected int bigMushroomsPerChunk;

	/** True if decorator should generate surface lava & water */
	public boolean generateLakes;

	protected int chunk_Y;
	
	protected int seaLevel;

	protected int maxTerrainY;

	protected int minTerrainY;

	public CubeBiomeDecorator()
	{
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
		this.field_150514_p = new WorldGenFlowers( Blocks.yellow_flower );
		this.mushroomBrownGen = new WorldGenFlowers( Blocks.brown_mushroom );
		this.mushroomRedGen = new WorldGenFlowers( Blocks.red_mushroom );
		this.bigMushroomGen = new WorldGenBigMushroom();
		this.reedGen = new WorldGenReed();
		this.cactusGen = new WorldGenCactus();
		this.waterlilyGen = new WorldGenWaterlily();
		this.flowersPerChunk = 2;
		this.grassPerChunk = 1;
		this.sandPerChunk = 1;
		this.sandPerChunk2 = 3;
		this.clayPerChunk = 1;
		this.generateLakes = true;
	}

	public void func_150512_a( World world, Random rand, CubeBiomeGenBase biome, int cubeX, int cubeZ )
	{
		throw new RuntimeException( "Can't decorate Columns!" );
	}

	public void decorate( World world, Random rand, CubeBiomeGenBase biome, int cubeX, int cubeY, int cubeZ )
	{
		if( this.currentWorld != null )
		{
			//In case if we generate something really high and poulate other cubes...
			if( this.chunk_X == cubeX && this.chunk_Z == cubeZ && this.chunk_Y < cubeY )
			{
				//do not populate this cube. We are already generatinbg something in this cube
				return;
			}
			throw new RuntimeException( "Already decorating!!" );
		}
		else
		{
			this.currentWorld = world;
			this.randomGenerator = rand;
			this.chunk_X = cubeX;
			this.chunk_Y = cubeY;
			this.chunk_Z = cubeZ;
			//TODO: move setting seaLevel and min/maxTerrainY to constructor
			this.seaLevel = ((CubeWorldProvider)world.provider).getSeaLevel();
			this.maxTerrainY = MathHelper.floor_double( NewTerrainProcessor.maxElev );
			//magic...
			this.minTerrainY = -maxTerrainY + 2 * seaLevel;
			this.decorate_do( biome );
			this.currentWorld = null;
			this.randomGenerator = null;
		}
	}

	protected void decorate_do( CubeBiomeGenBase biome )
	{
		this.generateOres();
		int xAbs;
		int yAbs;
		int zAbs;

		int minY = chunk_Y * 16 + 8;
		int maxY = minY + 16;

		int blockXCenter = this.chunk_X * 16 + 8;
		int blockZCenter = this.chunk_Z * 16 + 8;
		for( int i = 0; i < this.sandPerChunk2; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			yAbs = this.currentWorld.getTopSolidOrLiquidBlock( xAbs, zAbs );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.sandGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.clayPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			yAbs = this.currentWorld.getTopSolidOrLiquidBlock( xAbs, zAbs );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.clayGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.sandPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			yAbs = this.currentWorld.getTopSolidOrLiquidBlock( xAbs, zAbs );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.gravelAsSandGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		int trees = this.treesPerChunk;

		if( this.randomGenerator.nextInt( 10 ) == 0 )
		{
			++trees;
		}

		for( int i = 0; i < trees; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			yAbs = this.currentWorld.getHeightValue( xAbs, zAbs );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			WorldGenAbstractTree wGenTree = biome.checkSpawnTree( this.randomGenerator );
			wGenTree.setScale( 1.0D, 1.0D, 1.0D );

			if( wGenTree.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs ) )
			{
				wGenTree.func_150524_b( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
			}
		}

		for( int i = 0; i < this.bigMushroomsPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			yAbs = this.currentWorld.getHeightValue( xAbs, zAbs );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.bigMushroomGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.flowersPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs ) + 32;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			String name = biome.spawnFlower( this.randomGenerator, xAbs, yAbs, zAbs );
			BlockFlower block = BlockFlower.func_149857_e( name );

			if( block.getMaterial() != Material.air )
			{
				this.field_150514_p.func_150550_a( block, BlockFlower.func_149856_f( name ) );
				this.field_150514_p.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
			}
		}

		for( int i = 0; i < this.grassPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			//magic...
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			WorldGenerator generator = biome.getRandomWorldGenForGrass( this.randomGenerator );
			generator.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.deadBushPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			(new WorldGenDeadBush( Blocks.deadbush )).generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.waterlilyPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			while( yAbs > minTerrainY && this.currentWorld.isAirBlock( xAbs, yAbs - 1, zAbs ) )
			{
				--yAbs;
			}

			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.waterlilyGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < this.mushroomsPerChunk; ++i )
		{
			if( this.randomGenerator.nextInt( 4 ) == 0 )
			{
				xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
				zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
				yAbs = this.currentWorld.getHeightValue( xAbs, zAbs );
				if( yAbs >= minY || yAbs <= maxY )
				{
					this.mushroomBrownGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
				}
			}

			if( this.randomGenerator.nextInt( 8 ) == 0 )
			{
				xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
				zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
				int height = this.currentWorld.getHeightValue( xAbs, zAbs );
				height *= 2;
				height += minTerrainY;
				if( height > minTerrainY )
				{
					yAbs = rand( minTerrainY, height );
					if( yAbs >= minY || yAbs <= maxY )
					{
						this.mushroomRedGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
					}
				}
			}
		}

		if( this.randomGenerator.nextInt( 4 ) == 0 )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height > minTerrainY )
			{
				yAbs = rand( minTerrainY, height );
				if( yAbs >= minY && yAbs <= maxY )
				{
					this.mushroomBrownGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
				}
			}

		}

		if( this.randomGenerator.nextInt( 8 ) == 0 )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height > minTerrainY )
			{
				yAbs = rand( minTerrainY, height );
				if( yAbs >= minY && yAbs <= maxY )
				{
					this.mushroomRedGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
				}
			}

		}

		for( int i = 0; i < this.reedsPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.reedGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		for( int i = 0; i < 10; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.reedGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}

		if( this.randomGenerator.nextInt( 32 ) == 0 )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height > minTerrainY )
			{
				yAbs = rand( minTerrainY, height );
				if( yAbs >= minY || yAbs <= maxY )
				{
					(new WorldGenPumpkin()).generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
				}
			}

		}

		for( int i = 0; i < this.cactiPerChunk; ++i )
		{
			xAbs = blockXCenter + this.randomGenerator.nextInt( 16 );
			zAbs = blockZCenter + this.randomGenerator.nextInt( 16 );
			int height = this.currentWorld.getHeightValue( xAbs, zAbs );
			height *= 2;
			height += minTerrainY;
			if( height <= minTerrainY )
			{
				continue;
			}
			yAbs = rand( minTerrainY, height );
			if( yAbs < minY || yAbs > maxY )
			{
				continue;
			}
			this.cactusGen.generate( this.currentWorld, this.randomGenerator, xAbs, yAbs, zAbs );
		}
		//Don't do this yet. Will cubify later
		/*if( this.generateLakes )
		 {
		 for( xAbs = 0; xAbs < 50; ++xAbs )
		 {
		 zAbs = this.chunk_X + this.randomGenerator.nextInt( 16 ) + 8;
		 var5 = this.randomGenerator.nextInt( this.randomGenerator.nextInt( 248 ) + 8 );
		 var6 = this.chunk_Z + this.randomGenerator.nextInt( 16 ) + 8;
		 (new WorldGenLiquids( Blocks.flowing_water )).generate( this.currentWorld, this.randomGenerator, zAbs, var5, var6 );
		 }

		 for( xAbs = 0; xAbs < 20; ++xAbs )
		 {
		 zAbs = this.chunk_X + this.randomGenerator.nextInt( 16 ) + 8;
		 var5 = this.randomGenerator.nextInt( this.randomGenerator.nextInt( this.randomGenerator.nextInt( 240 ) + 8 ) + 8 );
		 var6 = this.chunk_Z + this.randomGenerator.nextInt( 16 ) + 8;
		 (new WorldGenLiquids( Blocks.flowing_lava )).generate( this.currentWorld, this.randomGenerator, zAbs, var5, var6 );
		 }
		 }*/
	}

	/**
	 * Standard ore generation helper. Generates most ores.
	 */
	protected void genStandardOre1( int numGen, double probability, WorldGenerator generator, double minHeight, double maxHeight, int maxTerrainHeight )
	{
		int minCubeY = CubeCoordinate.blockToCube( MathHelper.floor_double( minHeight * maxTerrainHeight + seaLevel ) );
		int maxCubeY = CubeCoordinate.blockToCube( MathHelper.floor_double( maxHeight * maxTerrainHeight + seaLevel ) );
		if( this.chunk_Y > maxCubeY || this.chunk_Y < minCubeY )
		{
			return;
		}
		for( int n = 0; n < numGen; ++n )
		{
			if( this.randomGenerator.nextDouble() < probability )
			{
				continue;
			}
			int x = this.chunk_X * 16 + 8 + this.randomGenerator.nextInt( 16 );
			int y = this.chunk_Y * 16 + 8 + this.randomGenerator.nextInt( 16 );
			int z = this.chunk_Z * 16 + 8 + this.randomGenerator.nextInt( 16 );
			generator.generate( this.currentWorld, this.randomGenerator, x, y, z );
		}
	}

	protected void genStandardOre1( int numGen, double probability, WorldGenerator generator, double maxHeight, int maxTerrainHeight )
	{
		this.genStandardOre1( numGen, probability, generator, -1, maxHeight, maxTerrainHeight );
	}

	protected void genStandardOre1( int numGen, double probability, WorldGenerator generator, int maxTerrainHeight )
	{
		this.genStandardOre1( numGen, probability, generator, -1, 1, maxTerrainHeight );
	}

	/**
	 * Generates ores in the current chunk
	 */
	@Override
	protected void generateOres()
	{
		//Base parbability. In vanilla ores are generated in the whole comumn at once, so probability togenerate them in one cube is 1/16.
		double probability = 1D / 16D;
		this.genStandardOre1( 20, probability, this.dirtGen, maxTerrainY );//0-256
		//Gravel generation disabled because of incredibly slow world saving.
		//this.genStandardOre1( 10, probability, this.gravelGen, maxTerrainY );//0-256
		//generate only in range 0-128. Doubled probability
		this.genStandardOre1( 20, probability * 2, this.coalGen, 1D, maxTerrainY );//0-128
		this.genStandardOre1( 20, probability * 4, this.ironGen, 0D, maxTerrainY );//0-64//only below sea level
		this.genStandardOre1( 2, probability * 8, this.goldGen, -0.5D, maxTerrainY );//0-32 //only below 1/4 of max height
		this.genStandardOre1( 8, probability * 16, this.redstoneGen, -0.75D, maxTerrainY );//0-16
		this.genStandardOre1( 1, probability * 16, this.diamondGen, -0.75D, maxTerrainY );//0-16
		this.genStandardOre1( 1, probability * 8, this.lapisGen, -0.5D, maxTerrainY );//0-32
	}
	private int rand( int min, int max )
	{
		return randomGenerator.nextInt( max - min ) + min;
	}
}
