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
package cuchaz.cubicChunks.generator;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;

public class PopulationProcessor extends CubeProcessor
{
	private final int m_seaLevel;
	public PopulationProcessor( String name, CubeWorldServer world, int batchSize )
	{
		super( name, world.getCubeProvider(), batchSize );
		m_seaLevel = world.getCubeWorldProvider().getSeaLevel();
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();

		if( !CubeProviderTools.cubesForPopulationExist( m_provider, cubeX, cubeY, cubeZ ) )
		{
			return false;
		}
		//BlockFalling.fallInstantly
		BlockFalling.field_149832_M = true;

		int xAbs = cubeX * 16;
		int yAbs = cubeY * 16;
		int zAbs = cubeZ * 16;

		CubeWorldServer world = (CubeWorldServer)cube.getWorld();
		CubeBiomeGenBase biome = (CubeBiomeGenBase)world.getBiomeGenForCoords( xAbs + 16, zAbs + 16 );

		Random rand = new Random( world.getSeed() );
		long rand1 = rand.nextLong() / 2L * 2L + 1L;
		long rand2 = rand.nextLong() / 2L * 2L + 1L;
		long rand3 = rand.nextLong() / 2L * 2L + 1L;
		rand.setSeed( (long)cubeX * rand1 + (long)cubeY * rand2 + (long)cubeZ * rand3 ^ world.getSeed() );

		boolean villageGenerated = false;
		//DON'T DO THIS YET
		/*if( m_mapFeaturesEnabled )
		 {
		 m_mineshaftGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		 var11 = m_villageGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		 m_strongholdGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		 m_scatteredFeatureGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		 }
		 */

		//Start from center of cube
		int xCenter = xAbs + 8;
		int yCenter = yAbs + 8;
		int zCenter = zAbs + 8;
		
		int genX;
		int genY;
		int genZ;
		
		if( biome != CubeBiomeGenBase.desert && biome != CubeBiomeGenBase.desertHills && !villageGenerated && rand.nextInt( 4 ) == 0 )
		{
			genX = xCenter + rand.nextInt( 16 );
			genY = yCenter + rand.nextInt( 16 );
			genZ = zCenter + rand.nextInt( 16 );
			(new WorldGenLakes( Blocks.water )).generate( world, rand, genX, genY, genZ );
		}
		
		if( !villageGenerated && rand.nextInt( 8 ) == 0 )
		{
			//var13 = m_rand.nextInt( m_rand.nextInt( 248 ) + 8 );

			if( rand.nextInt( Math.max( 1, cubeY + 8 - m_seaLevel / 16 ) ) == 0 )
			{
				genX = xCenter + rand.nextInt( 16 );
				genY = yCenter + rand.nextInt( 16 );
				genZ = zCenter + rand.nextInt( 16 );

				if( genY < m_seaLevel || rand.nextInt( 10 ) == 0 )
				{
					(new WorldGenLakes( Blocks.lava )).generate( world, rand, genX, genY, genZ );
				}
			}
		}
		//really? instead of making a new var here, they reuse var12 and make a new var15 instead of how they were doing it the previous two times
		// ^ code obfuscator did it
		/*for( var12 = 0; var12 < 8; ++var12 ) 
		{
			var13 = xAbs + m_rand.nextInt( 16 ) + 8;
			var14 = m_rand.nextInt( 256 );
			int var15 = zAbs + m_rand.nextInt( 16 ) + 8;
			( new WorldGenDungeons() ).generate( m_world, m_rand, var13, var14, var15 );
		}*/
		for( int i = 0; i < 8; ++i )
		{
			if( rand.nextInt( 16 ) != 0 )
			{
				continue;
			}
			genX = xCenter + rand.nextInt( 16 );
			genY = yCenter + rand.nextInt( 16 );
			genZ = zCenter + rand.nextInt( 16 );
			(new WorldGenDungeons()).generate( world, rand, genX, genY, genZ );
		}
		
		//biome.decorate( world, rand, cubeX, cubeY, cubeZ );
		//TODO: cubify this:
		//SpawnerAnimals.performWorldGenSpawning( m_world, var6, xAbs + 8, zAbs + 8, 16, 16, m_rand );

		int xCenterPlus16 = xCenter + 16;
		int zCenterPlus16 = xCenter + 16;
		for( int x = xCenter; x < xCenterPlus16; x++ )
		{
			for( int z = zCenter; z < zCenterPlus16; z++ )
			{
				int y = world.getPrecipitationHeight( x, z ) - 1;
				if( Coords.blockToCube( y ) != cubeY )
				{
					continue;
				}
				if( world.isBlockFreezable( x, y, z ) )
				{
					world.setBlock( x, y, z, Blocks.ice, 0, 2 );
				}
				if( world.func_147478_e( x, y, z, true ) )
				{
					world.setBlock( x, y + 1, z, Blocks.snow_layer, 0, 2 );
				}
			}
		}

		BlockFalling.field_149832_M = false;
		
		return true;
	}
}