/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 *     Bartosz Skrzypczak - Reverse engineered Vanilla's replaceBlocksForBiome
 *     		and created a cubified version.
 ******************************************************************************/
package cuchaz.cubicChunks.generator;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.CubeWorld;
import static cuchaz.cubicChunks.generator.GeneratorStage.Biomes;
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
	
	private int seaLevel; 
	
	public BiomeProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );
		
		m_worldServer = worldServer;
		
		m_rand = new Random( worldServer.getSeed() );
		m_noiseGen = new NoiseGeneratorPerlin( m_rand, 4 );
		m_noise = new double[256];
		m_biomes = null;
		
		seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();
	}
	
	@Override
	public boolean calculate( Cube cube )
	{
		//uncomment line below to disable dirt/grass generation
		//if(true) return true;
		// only continue if the neighboring cubes exist
		CubeProvider provider = ((CubeWorld)cube.getWorld()).getCubeProvider();
		if( !CubeProviderTools.cubeAndNeighborsExist(provider, cube.getX(), cube.getY(), cube.getZ() ) )
		{
			return false;
		}

		//Nothing to do...
		if( cube.isEmpty() )
		{
			return true;
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
			16, 16, 0.06D, 0.06D, 1
		);
		
		Cube above = provider.provideCube(cube.getX(), cube.getY() + 1, cube.getZ());
		//Do not modify cube below
		//Cube below = provider.provideCube(cube.getX(), cube.getY() - 1, cube.getZ());
		
		int topOfCube = Coords.cubeToMaxBlock(cube.getY());
		int topOfCubeAbove = Coords.cubeToMaxBlock(cube.getY() + 1);
		int bottomOfCube = Coords.cubeToMinBlock(cube.getY());
		
		// already checked that cubes above and below exist
		// Do not modify cubes above/below to avoid generating dirt/grass in caves
		int alterationTop = topOfCube;
		int top = topOfCubeAbove;
		int bottom = bottomOfCube;
		
		for( int xRel = 0; xRel < 16; xRel++ )
		{
			int xAbs = cube.getX() << 4 | xRel;
			
			for( int zRel = 0; zRel < 16; zRel++ )
			{
				int zAbs = cube.getZ() << 4 | zRel;
				
				int xzCoord = zRel << 4 | xRel;
				
				CubeBiomeGenBase biome = m_biomes[xzCoord];
				
				//Biome blocks depth in current block column. 0 for negative values.

				biome.replaceBlocks( m_worldServer, m_rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, m_noise[zRel * 16 + xRel] );
			}
		}	
		
		return true;
	}
}
