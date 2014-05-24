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
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;

public class PopulationProcessor extends CubeProcessor
{
	public PopulationProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		/* TEMP: don't do population yet
		
        // field_149832_M = fallInstantly
		BlockFalling.field_149832_M = true;
		int xAbs = cubeX * 16;
		int yAbs = cubeY * 16;
		int zAbs = cubeZ * 16;
		BiomeGenBase var6 = m_world.getBiomeGenForCoords( xAbs + 16, zAbs + 16 );
		m_rand.setSeed( m_world.getSeed() );
		long var7 = m_rand.nextLong() / 2L * 2L + 1L;
		long var9 = m_rand.nextLong() / 2L * 2L + 1L;
		m_rand.setSeed( (long)cubeX * var7 + (long)cubeZ * var9 ^ m_world.getSeed() );
		boolean var11 = false;
		
		if( m_mapFeaturesEnabled )
		{
			m_mineshaftGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			var11 = m_villageGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			m_strongholdGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
			m_scatteredFeatureGenerator.generateStructuresInChunk( m_world, m_rand, cubeX, cubeZ );
		}
		
		int var12;
		int var13;
		int var14;
		
		if( var6 != BiomeGenBase.desert && var6 != BiomeGenBase.desertHills && !var11 && m_rand.nextInt( 4 ) == 0 )
		{
			var12 = xAbs + m_rand.nextInt( 16 ) + 8;
			var13 = m_rand.nextInt( 256 ); // randomly picks a y value
			var14 = zAbs + m_rand.nextInt( 16 ) + 8;
			( new WorldGenLakes( Blocks.water ) ).generate( m_world, m_rand, var12, var13, var14 );
		}
		
		if( !var11 && m_rand.nextInt( 8 ) == 0 )
		{
			var12 = xAbs + m_rand.nextInt( 16 ) + 8;
			var13 = m_rand.nextInt( m_rand.nextInt( 248 ) + 8 );
			var14 = zAbs + m_rand.nextInt( 16 ) + 8;
			
			if( var13 < 63 || m_rand.nextInt( 10 ) == 0 )
			{
				( new WorldGenLakes( Blocks.lava ) ).generate( m_world, m_rand, var12, var13, var14 );
			}
		}
		
		for( var12 = 0; var12 < 8; ++var12 ) //really? instead of making a new var here, they reuse var12 and make a new var15 instead of how they were doing it the previous two times
		{
			var13 = xAbs + m_rand.nextInt( 16 ) + 8;
			var14 = m_rand.nextInt( 256 );
			int var15 = zAbs + m_rand.nextInt( 16 ) + 8;
			( new WorldGenDungeons() ).generate( m_world, m_rand, var13, var14, var15 );
		}
		
		var6.decorate( m_world, m_rand, xAbs, zAbs );
		SpawnerAnimals.performWorldGenSpawning( m_world, var6, xAbs + 8, zAbs + 8, 16, 16, m_rand );
		xAbs += 8;
		zAbs += 8;
		
		for( var12 = 0; var12 < 16; ++var12 )
		{
			for( var13 = 0; var13 < 16; ++var13 )
			{
				var14 = m_world.getPrecipitationHeight( xAbs + var12, zAbs + var13 );
				
				if( m_world.isBlockFreezable( var12 + xAbs, var14 - 1, var13 + zAbs ) )
				{
					m_world.setBlock( var12 + xAbs, var14 - 1, var13 + zAbs, Blocks.ice, 0, 2 );
				}
				
				if( m_world.func_147478_e( var12 + xAbs, var14, var13 + zAbs, true ) )
				{
					m_world.setBlock( var12 + xAbs, var14, var13 + zAbs, Blocks.snow_layer, 0, 2 );
				}
			}
		}
		
		BlockFalling.field_149832_M = false;
		*/
		
		return true;
	}
}
