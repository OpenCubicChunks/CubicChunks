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
		// only continue if the neighboring cubes exist
		CubeProvider provider = ((CubeWorld)cube.getWorld()).getCubeProvider();
		if( !CubeProviderTools.cubeAboveExistsAndStageEqualOrHigher( provider, Biomes, cube.getX(), cube.getY(), cube.getZ() ) )
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
			16, 16, 16, 16, 1
		);
		
		Cube above = provider.provideCube(cube.getX(), cube.getY() + 1, cube.getZ());
		//Do not modify cube below
		//Cube below = provider.provideCube(cube.getX(), cube.getY() - 1, cube.getZ());
		
		int topOfCube = Coords.cubeToMaxBlock(cube.getY());
		int bottomOfCube = Coords.cubeToMinBlock(cube.getY());
		
		// already checked that cubes above and below exist
		// Do not modyfy cubes above/below to avoid generating dirt/grass in caves
		int alterationTop = topOfCube;
		int top = topOfCube + 8;
		int bottom = bottomOfCube/* - 8*/;
		
		for( int xRel = 0; xRel < 16; xRel++ )
		{
			int xAbs = cube.getX() << 4 | xRel;
			
			for( int zRel = 0; zRel < 16; zRel++ )
			{
				int zAbs = cube.getZ() << 4 | zRel;
				
				int xzCoord = zRel << 4 | xRel;
				
				CubeBiomeGenBase biome = m_biomes[xzCoord];
				
				//Biome blocks depth in current block column. 0 for negative values.
				
				int depth = ( int ) ( m_noise[zRel + xRel * 16] / 3D + 3D + m_rand.nextDouble() * 0.25D );
				
				//How many biome blocks left to set in column? Initially -1
				int numBlocksToChange = -1;
				
				Block topBlock = biome.topBlock;
				Block fillerBlock = biome.fillerBlock;

				for ( int yAbs = top; yAbs >= bottom; --yAbs )
				{
					//Current block
					Block block = replaceBlocksForBiome_getBlock(cube, above, xAbs, yAbs, zAbs );
					
					//Set numBlocksToChange to -1 when we reach air, skip everything else
					if ( block == Blocks.air )
					{
						numBlocksToChange = -1;
						continue;
					}

					//Do not replace any blocks axcept already replaced and stone
					if (block != Blocks.stone && block != biome.topBlock && block != biome.fillerBlock && block != Blocks.sandstone)
					{
						continue;
					}
					
					//If we are 1 block below air...
					if ( numBlocksToChange == -1 )
					{
						//If depth is <= 0 - only stone
						if ( depth <= 0 )
						{
							topBlock = Blocks.air;
							fillerBlock = Blocks.stone;
						}
						//If we are above or at 4 block under water and at or below one block above water
						else if ( yAbs >= seaLevel - 4 && yAbs <= seaLevel + 1 )
						{
							topBlock = biome.topBlock;
							fillerBlock = biome.fillerBlock;
						}
						//If top block is air and we are below sea level use water instead
						if ( yAbs < seaLevel && topBlock == Blocks.air )
						{
							topBlock = Blocks.water;
						}
						//Set num blocks to change to current depth.
						numBlocksToChange = depth;

						//Modify blocks only if we are at or below alteration top
						if ( yAbs <= alterationTop )
						{
							if ( yAbs >= seaLevel - 1 )
							{
								//If we are above sea level
								replaceBlocksForBiome_setBlock(topBlock, cube, xAbs, yAbs, zAbs);
							} 
							else
							{
								//Don't set grass underwater
								replaceBlocksForBiome_setBlock(fillerBlock, cube, xAbs, yAbs, zAbs);
							}
						}

						continue;
					}
					// Nothing left to do...
					// so continue
					if ( numBlocksToChange <= 0 )
					{
						continue;
					}
					//Decrease blocks to change
					numBlocksToChange--;

					//Set blocks only if we are below or at alteration top
					if ( yAbs <= alterationTop )
					{
						replaceBlocksForBiome_setBlock(fillerBlock, cube, xAbs, yAbs, zAbs);
					}

					// random sandstone generation
					if ( numBlocksToChange == 0 && fillerBlock == Blocks.sand )
					{
						numBlocksToChange = m_rand.nextInt( 4 );
						fillerBlock = Blocks.sandstone;
					}
				}
			}
		}	
		
		return true;
	}
	
	private void replaceBlocksForBiome_setBlock(Block block, Cube cube, int xAbs, int yAbs, int zAbs)
	{
		assert Coords.blockToCube( yAbs ) == cube.getY() || Coords.blockToCube( yAbs ) == cube.getY() - 1;
		
		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;
		
		assert Coords.blockToCube( yAbs ) == cube.getY() ;
		cube.setBlockForGeneration( xRel, yRel, zRel, block );
	}
	
	private Block replaceBlocksForBiome_getBlock(Cube cube, Cube above, int xAbs, int yAbs, int zAbs)
	{
		assert m_worldServer.getCubeProvider().cubeExists( Coords.blockToCube( xAbs ), Coords.blockToCube( yAbs ), Coords.blockToCube( zAbs ) );
		
		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;
		
		if(Coords.blockToCube( yAbs ) == cube.getY()) // check if we're in the same cube as Cube
		{
			//If we are in the same cube
			return cube.getBlock( xRel, yRel, zRel );
		} 
		else 
		{
			//we are in cube above
			assert Coords.blockToCube( yAbs ) == cube.getY() + 1;
			return above.getBlock( xRel, yRel, zRel );
		}
	}
}
