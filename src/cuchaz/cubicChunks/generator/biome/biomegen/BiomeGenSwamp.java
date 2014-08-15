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

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import cuchaz.cubicChunks.world.Cube;

public class BiomeGenSwamp extends CubeBiomeGenBase
{
	protected BiomeGenSwamp( int par1 )
	{
		super( par1 );

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn( 2 );
		cfg.flowersPerColumn( 1 );
		cfg.deadBushPerColumn( 1 );
		cfg.mushroomsPerColumn( 8 );
		cfg.reedsPerColumn( 10 );
		cfg.clayPerColumn( 1 );
		cfg.waterlilyPerColumn( 4 );
		cfg.sandPerColumn( 0 );
		cfg.gravelPerColumn( 0 );
		cfg.grassPerColumn( 5 );
		this.waterColorMultiplier = 14745518;
		this.spawnableMonsterList.add( new CubeBiomeGenBase.SpawnListEntry( EntitySlime.class, 1, 1, 1 ) );
	}

	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return this.worldGeneratorSwamp;
	}

	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeGrassColor( int p_150558_1_, int p_150558_2_, int p_150558_3_ )
	{
		double var4 = field_150606_ad.func_151601_a( (double)p_150558_1_ * 0.0225D, (double)p_150558_3_ * 0.0225D );
		return var4 < -0.1D ? 5011004 : 6975545;
	}

	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeFoliageColor( int p_150571_1_, int p_150571_2_, int p_150571_3_ )
	{
		return 6975545;
	}

	@Override
	public String spawnFlower( Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_ )
	{
		return BlockFlower.field_149859_a[1];
	}

	@Override
	public void decorate( World world, Random rand, int cubeX, int cubeY, int cubeZ )
	{

		int seaLevel = ((CubeWorldServer)world).getCubeWorldProvider().getSeaLevel();
		int minY = Coords.localToBlock( cubeY, 8 );
		int maxY = minY + 16;

		//moved from replaceBlocks
		for( int x = 0; x < 16; x++ )
		{
			int xAbs = Coords.localToBlock( cubeX, x );
			for( int z = 0; z < 16; z++ )
			{
				int zAbs = Coords.localToBlock( cubeZ, z );
				
				double var9 = field_150606_ad.func_151601_a( (double)xAbs * 0.25D, (double)zAbs * 0.25D );
				if( var9 > 0.0D )
				{
					for( int y = maxY; y >= minY; --y )
					{
						int yAbs = Coords.localToBlock(cubeY, y);
						Block block = world.getBlock( xAbs, yAbs, zAbs );

						if( block == null || block.getMaterial() != Material.air )
						{
							if( yAbs == seaLevel - 1 && block != Blocks.water )
							{
								world.setBlock( xAbs, yAbs, zAbs, Blocks.water );

								if( var9 < 0.12D )
								{
									world.setBlock( xAbs, yAbs + 1, zAbs, Blocks.waterlily );
								}
							}

							break;
						}
					}
				}
			}
		}
		super.decorate( world, rand, cubeX, cubeY, cubeZ );
	}
}
