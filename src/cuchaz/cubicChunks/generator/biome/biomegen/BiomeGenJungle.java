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
import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenMegaJungleCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenShrubCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTallGrassCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTreesCube;
import java.util.Random;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMelon;

public class BiomeGenJungle extends CubeBiomeGenBase
{
	private final boolean type;

	public BiomeGenJungle( int id, boolean type )
	{
		super( id );
		this.type = type;

		if( type )
		{
			this.decorator().treesPerChunk = 2;
		}
		else
		{
			this.decorator().treesPerChunk = 50;
		}

		this.decorator().grassPerChunk = 25;
		this.decorator().flowersPerChunk = 4;

		if( !type )
		{
			this.spawnableMonsterList.add( new CubeBiomeGenBase.SpawnListEntry( EntityOcelot.class, 2, 1, 1 ) );
		}

		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityChicken.class, 10, 4, 4 ) );
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return (p_150567_1_.nextInt( 10 ) == 0 ? this.worldGeneratorBigTree : (p_150567_1_.nextInt( 2 ) == 0 ? new WorldGenShrubCube( 3, 0 ) : (!this.type && p_150567_1_.nextInt( 3 ) == 0 ? new WorldGenMegaJungleCube( false, 10, 20, 3, 3 ) : new WorldGenTreesCube( false, 4 + p_150567_1_.nextInt( 7 ), 3, 3, true ))));
	}

	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	@Override
	public WorldGeneratorCube getRandomWorldGenForGrass( Random par1Random )
	{
		return par1Random.nextInt( 4 ) == 0 ? new WorldGenTallGrassCube( Blocks.tallgrass, 2 ) : new WorldGenTallGrassCube( Blocks.tallgrass, 1 );
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		super.decorate( world, rand, x, y, z );
		
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		gen.generateAtRandSurfacePlus32( new WorldGenMelon(), 1, 1 );
	}
}
