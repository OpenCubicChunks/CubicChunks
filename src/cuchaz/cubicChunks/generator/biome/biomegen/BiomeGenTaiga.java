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
import cuchaz.cubicChunks.generator.populator.generators.WorldGenMegaPineTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga1Cube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga2Cube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTallGrassCube;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBlockBlob;

public class BiomeGenTaiga extends CubeBiomeGenBase
{
	private static final WorldGenTaiga1Cube wGenTree1 = new WorldGenTaiga1Cube();
	private static final WorldGenTaiga2Cube wGenTree2 = new WorldGenTaiga2Cube( false );
	private static final WorldGenMegaPineTreeCube wGenMegaPineTree1 = new WorldGenMegaPineTreeCube( false, false );
	private static final WorldGenMegaPineTreeCube wGenMegaPineTree2 = new WorldGenMegaPineTreeCube( false, true );
	private static final WorldGenBlockBlob wGenBlockBlob = new WorldGenBlockBlob( Blocks.mossy_cobblestone, 0 );
	private final int type;

	public BiomeGenTaiga( int id, int type )
	{
		super( id );
		this.type = type;
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityWolf.class, 8, 4, 4 ) );
		this.decorator().treesPerChunk = 10;

		if( type != 1 && type != 2 )
		{
			this.decorator().grassPerChunk = 1;
			this.decorator().mushroomsPerChunk = 1;
		}
		else
		{
			this.decorator().grassPerChunk = 7;
			this.decorator().deadBushPerChunk = 1;
			this.decorator().mushroomsPerChunk = 3;
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return ((this.type == 1 || this.type == 2) && p_150567_1_.nextInt( 3 ) == 0 ? (this.type != 2 && p_150567_1_.nextInt( 13 ) != 0 ? wGenMegaPineTree1 : wGenMegaPineTree2) : (p_150567_1_.nextInt( 3 ) == 0 ? wGenTree1 : wGenTree2));
	}

	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	@Override
	public WorldGeneratorCube getRandomWorldGenForGrass( Random par1Random )
	{
		return par1Random.nextInt( 5 ) > 0 ? new WorldGenTallGrassCube( Blocks.tallgrass, 2 ) : new WorldGenTallGrassCube( Blocks.tallgrass, 1 );
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z );
		
		if( this.type == 1 || this.type == 2 )
		{
			gen.generateAtSurface( wGenBlockBlob, rand.nextInt( 3 ), 1);
		}

		worldGenDoublePlant.setType( 3 );
		gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 7, 1);

		super.decorate( world, rand, x, y, z );
	}

	@Override
	public void modifyBlocks_pre( World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double val )
	{
		if( this.type == 1 || this.type == 2 )
		{
			this.topBlock = Blocks.grass;
			this.field_150604_aj = 0;
			this.fillerBlock = Blocks.dirt;

			if( val > 1.75D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 1;
			}
			else if( val > -0.95D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 2;
			}
		}

		this.modifyBlocks( world, rand, cube, xAbs, yAbs, zAbs, val );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		return this.biomeID == CubeBiomeGenBase.megaTaiga.biomeID ? (new BiomeGenTaiga( this.biomeID + 128, 2 )).func_150557_a( 5858897, true ).setBiomeName( "Mega Spruce Taiga" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.25F, 0.8F ).setHeightRange( new CubeBiomeGenBase.Height( this.biomeHeight, this.biomeVolatility ) ) : super.createAndReturnMutated();
	}
}
