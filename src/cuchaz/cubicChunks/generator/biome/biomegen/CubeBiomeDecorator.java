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
	/** True if decorator should generate surface lava & water */
	public boolean generateLakes;

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

	private WorldGenFlowersCube flowerGen;
	
	protected DecoratorHelper gen;

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
		this.flowerGen = new WorldGenFlowersCube( Blocks.yellow_flower );
		this.mushroomBrownGen = new WorldGenFlowersCube( Blocks.brown_mushroom );
		this.mushroomRedGen = new WorldGenFlowersCube( Blocks.red_mushroom );
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
			this.gen = new DecoratorHelper(world, rand, cubeX, cubeY, cubeZ);
			this.randomGenerator = rand;
			this.decorate_do( biome );
			this.gen = null;
		}
	}

	protected void decorate_do( CubeBiomeGenBase biome )
	{
		this.generateOres();

		gen.generateAtSurface( this.sandGen, this.sandPerChunk2, 1 );
		gen.generateAtSurface( this.clayGen, this.clayPerChunk, 1 );
		gen.generateAtSurface( this.gravelAsSandGen, this.sandPerChunk, 1 );

		int trees = this.randomGenerator.nextInt( 10 ) == 0 ? this.treesPerChunk + 1 : this.treesPerChunk;

		for( int i = 0; i < trees; ++i )
		{
			WorldGenAbstractTreeCube wGenTree = biome.checkSpawnTree( this.randomGenerator );
			wGenTree.setScale( 1.0D, 1.0D, 1.0D );
			gen.generateAtSurface( wGenTree, 1, 1 );
		}

		gen.generateAtSurface( this.bigMushroomGen, this.bigMushroomsPerChunk, 1 );

		gen.generateFlowers( flowerGen, biome, this.flowersPerChunk, 1 );

		gen.generateAtRand2xHeight1( biome.getRandomWorldGenForGrass( this.randomGenerator ), this.grassPerChunk, 1 );
		gen.generateAtRand2xHeight1( new WorldGenDeadBushCube( Blocks.deadbush ), this.deadBushPerChunk, 1 );

		gen.generateAtRand2xHeight2( this.waterlilyGen, this.waterlilyPerChunk, 1 );

		gen.generateAtSurface( this.mushroomBrownGen, this.mushroomsPerChunk, 0.25D );

		gen.generateAtRand2xHeight3( this.mushroomRedGen, this.mushroomsPerChunk + 1, 0.125D );
		gen.generateAtRand2xHeight3( this.mushroomBrownGen, 1, 0.25D );
		gen.generateAtRand2xHeight3( this.reedGen, reedsPerChunk <= 0 ? 10 : reedsPerChunk + 10, 1 );
		gen.generateAtRand2xHeight3( new WorldGenPumpkin(), 1, 1.0D / 32.0D );
		gen.generateAtRand2xHeight3( this.cactusGen, this.cactiPerChunk, 1 );

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

}
