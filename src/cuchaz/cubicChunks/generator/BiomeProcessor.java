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

import java.util.Random;

import net.minecraft.world.gen.NoiseGeneratorPerlin;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.CubeWorld;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;

public class BiomeProcessor extends CubeProcessor
{
	private CubeWorldServer m_worldServer;
	
	private Random m_rand;
	private NoiseGeneratorPerlin m_noiseGen;
	private double[] m_noise;
	private CubeBiomeGenBase[] m_biomes;
	
	public BiomeProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );
		
		m_worldServer = worldServer;
		
		m_rand = new Random( worldServer.getSeed() );
		m_noiseGen = new NoiseGeneratorPerlin( m_rand, 4 );
		m_noise = new double[256];
		m_biomes = null;
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		// only continue if the neighboring cubes exist
		CubeProvider provider = ((CubeWorld)cube.getWorld()).getCubeProvider();
		if( !CubeProviderTools.cubeAndNeighborsExist( provider, cube.getX(), cube.getY(), cube.getZ() ) )
		{
			return false;
		}
		
		// generate biome info. This is a hackjob.
		m_biomes = (CubeBiomeGenBase[])m_worldServer.getCubeWorldProvider().getWorldColumnMananger().loadBlockGeneratorData(
			m_biomes,
			Coords.cubeToMinBlock( cube.getX() ), Coords.cubeToMinBlock( cube.getZ() ),
			16, 16
		);
		
		m_noise = m_noiseGen.func_151599_a(
			m_noise,
			Coords.cubeToMinBlock( cube.getX() ), Coords.cubeToMinBlock( cube.getZ() ),
			16, 16, 16, 16, 1
		);
		
		for( int xRel = 0; xRel < 16; xRel++ )
		{
			int xAbs = cube.getX() << 4 | xRel;
			
			for( int zRel = 0; zRel < 16; zRel++ )
			{
				int zAbs = cube.getZ() << 4 | zRel;
				
				int xzCoord = zRel | xRel << 4;

				for (int yRel = 0; yRel < 16; yRel++)
				{	
					int yAbs = cube.getY() << 4 | yRel;
					
					m_biomes[xzCoord].modifyBlocks_pre(
						m_worldServer, m_rand,
						cube,
						xAbs, yAbs, zAbs,
						m_noise[xzCoord]
					);
				}
			}
		}
		
		return true;
	}
}
