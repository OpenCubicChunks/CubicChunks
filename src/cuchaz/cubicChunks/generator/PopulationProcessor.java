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

import cuchaz.cubicChunks.CubeProviderTools;
import cuchaz.cubicChunks.generator.biome.alternateGen.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Column;
import cuchaz.cubicChunks.world.Cube;
import cuchaz.cubicChunks.world.LightIndex;

import java.util.Random;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.feature.WorldGenMinable;

public class PopulationProcessor extends CubeProcessor
{
	private final int m_seaLevel;

	public PopulationProcessor( String name, CubeWorldServer world, int batchSize )
	{
		super( name, world.getCubeProvider(), batchSize );
		m_seaLevel = world.getCubeWorldProvider().getSeaLevel();
	}

	@Override
	public boolean calculate( Cube cube)
	{
		CubeWorldServer world = (CubeWorldServer)cube.getWorld();
		AlternateWorldColumnManager wcm = (AlternateWorldColumnManager)world.getWorldChunkManager();
		double [][] height = wcm.getHeightArray(cube.getX(), cube.getZ());
		//double [][] volatility = wcm.getVolArray(cube.getX(), cube.getZ());
		double [][] temp = wcm.getTempArray(cube.getX(), cube.getZ());
		double [][] hum = wcm.getRainfallArray(cube.getX(), cube.getZ());
		int heightMax = Integer.MIN_VALUE;
		int heightMin = Integer.MAX_VALUE;
		double volMax = 0;
		for (int i = 0; i < 16; i++){
			for (int j = 0; j < 16; j++){
				int height1 = (int)wcm.getRealHeight(height[i][j],temp[i][j],hum[i][j]);
				//double volatility1 = wcm.getRealVolatility(volatility[i][j], height[i][j], hum[i][j], temp[i][j]);
				heightMax = heightMax > height1 ? heightMax : height1 ;
				heightMin = heightMin < height1 ? heightMin : height1 ;
				//volMax = volMax > volatility1 ? volMax : volatility1 ;
			}
		}
		int buffer = 30;// +/-3 cubes
		if ( cube.getY() * 2 < heightMin - buffer){return calculateUnderground(cube, world, wcm);}
		if ( cube.getY() * 2 > heightMax + buffer){return calculateSky(cube, world, wcm);}
		return calculateSurface( cube, world, wcm);
	}
	
	public boolean calculateUnderground( Cube cube, CubeWorldServer world, AlternateWorldColumnManager wcm)
	{
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();

		//Actually we don't need all neightbor cubes, but to be sure generation order is correct...
		if( !CubeProviderTools.cubeAndNeighborsExist( m_provider, cubeX, cubeY, cubeZ ) )
		{
			return false;
		}

		if( !CubeProviderTools.checkGenerationStage( m_provider, GeneratorStage.Population, cubeX, cubeY, cubeZ, cubeX + 1, cubeY + 1, cubeZ + 1 ) )
		{
			return false;
		}
		//BlockFalling.fallInstantly
		BlockFalling.field_149832_M = true;

		int xAbs = cubeX * 16;
		int yAbs = cubeY * 16;
		int zAbs = cubeZ * 16;

		//This can be easily replaced with an different biome definition for underground decoration
		//CubeBiomeGenBase biome = (CubeBiomeGenBase)world.getBiomeGenForCoords( xAbs + 16, zAbs + 16 );
				
		Random rand = new Random( world.getSeed() );
		long rand1 = rand.nextLong() / 2L * 2L + 1L;
		long rand2 = rand.nextLong() / 2L * 2L + 1L;
		long rand3 = rand.nextLong() / 2L * 2L + 1L;
		rand.setSeed( (long)cubeX * rand1 + (long)cubeY * rand2 + (long)cubeZ * rand3 ^ world.getSeed() );

		
		//Start from center of cube
		int xCenter = xAbs + 8;
		int yCenter = yAbs + 8;
		int zCenter = zAbs + 8;

		int genX;
		int genY;
		int genZ;

		if( rand.nextInt( 16 ) == 0 )
		{
			genX = xCenter + rand.nextInt( 16 );
			genY = yCenter + rand.nextInt( 16 );
			genZ = zCenter + rand.nextInt( 16 );
			(new WorldGenLakes( Blocks.water )).generate( world, rand, genX, genY, genZ );
		}


		if(rand.nextInt( 8 ) == 0 )
		{
			if( rand.nextInt( Math.max( 1, cubeY + 16 - m_seaLevel / 16 ) ) == 0 )
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
		//Base parbability. In vanilla ores are generated in the whole comumn at once, so probability togenerate them in one cube is 1/16.
		double probability = 1D / 16D;
		DecoratorHelper gen = new DecoratorHelper( world, rand, cubeX, cubeY, cubeZ );
		WorldGenMinable dirtGen = new WorldGenMinable( Blocks.dirt, 32 );
		//WorldGenMinable gravelGen = new WorldGenMinable( Blocks.gravel, 32 );
		WorldGenMinable coalGen = new WorldGenMinable( Blocks.coal_ore, 16 );
		WorldGenMinable ironGen = new WorldGenMinable( Blocks.iron_ore, 8 );
		WorldGenMinable goldGen = new WorldGenMinable( Blocks.gold_ore, 8 );
		WorldGenMinable redstoneGen = new WorldGenMinable( Blocks.redstone_ore, 7 );
		WorldGenMinable diamondGen = new WorldGenMinable( Blocks.diamond_ore, 7 );
		WorldGenMinable lapisGen = new WorldGenMinable( Blocks.lapis_ore, 6 );
		gen.genberateAtRandomHeight( 20, probability, dirtGen );//0-256
		//Gravel generation disabled because of incredibly slow world saving.
		//this.generateAtRandomHeight( 10, probability, this.gravelGen, maxTerrainY );//0-256
		//generate only in range 0-128. Doubled probability
		gen.generateAtRandomHeight( 20, probability * 2, coalGen, 1.0D );//0-128
		gen.generateAtRandomHeight( 20, probability * 4, ironGen, 0.0D );//0-64//only below sea level
		gen.generateAtRandomHeight( 2, probability * 8, goldGen, -0.5D );//0-32 //only below 1/4 of max height
		gen.generateAtRandomHeight( 8, probability * 16, redstoneGen, -0.75D );//0-16
		gen.generateAtRandomHeight( 1, probability * 16, diamondGen, -0.75D );//0-16
		gen.generateAtRandomHeight( 1, probability * 8, lapisGen, -0.5D );//0-32


		return true;
	}
	
	public boolean calculateSky( Cube cube, CubeWorldServer world, AlternateWorldColumnManager wcm)
	{
		return true;
	}
	
	public boolean calculateSurface( Cube cube , CubeWorldServer world, AlternateWorldColumnManager wcm)
	{
		//uncomment line below to disable populator
		//if(true) return true;
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();

		//Actually we don't need all neightbor cubes, but to be sure generation order is correct...
		if( !CubeProviderTools.cubeAndNeighborsExist( m_provider, cubeX, cubeY, cubeZ ) )
		{
			return false;
		}

		if( !CubeProviderTools.checkGenerationStage( m_provider, GeneratorStage.Population, cubeX, cubeY, cubeZ, cubeX + 1, cubeY + 1, cubeZ + 1 ) )
		{
			return false;
		}
		//BlockFalling.fallInstantly
		BlockFalling.field_149832_M = true;

		int xAbs = cubeX * 16;
		int yAbs = cubeY * 16;
		int zAbs = cubeZ * 16;

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
			if( rand.nextInt( 16 ) == 0 )
			{
				genX = xCenter + rand.nextInt( 16 );
				genY = yCenter + rand.nextInt( 16 );
				genZ = zCenter + rand.nextInt( 16 );
				(new WorldGenLakes( Blocks.water )).generate( world, rand, genX, genY, genZ );
			}
		}

		if( !villageGenerated && rand.nextInt( 8 ) == 0 )
		{
			//var13 = m_rand.nextInt( m_rand.nextInt( 248 ) + 8 );

			if( rand.nextInt( Math.max( 1, cubeY + 16 - m_seaLevel / 16 ) ) == 0 )
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

		biome.decorate( world, rand, cubeX, cubeY, cubeZ );
		//TODO: cubify this:
		//SpawnerAnimals.performWorldGenSpawning( m_world, var6, xAbs + 8, zAbs + 8, 16, 16, m_rand );

		double[][] temps = wcm.getTempArray(cubeX, cubeZ);
		for( int x = 0; x < 16; x++ )
		{
			for( int z = 0; z < 16; z++ )
			{
				LightIndex c = cube.getColumn().getLightIndex();
				if (c == null){continue;}
				try {
					int y = c.getTopNonTransparentBlockY(x,z);
					
					if( Coords.blockToCube( y ) != cubeY )
					{
						continue;
					}
					//freeze the ocean/lakes
					if( cube.getBlock( x, Coords.blockToLocal(y), z ) == Blocks.water && temps[x][z] < 0.3D && y > m_seaLevel )
					{
						cube.setBlock( x, Coords.blockToLocal(y), z, Blocks.ice, 0 );
					}
					//freeze pools
					else if (cube.getBlock( x, Coords.blockToLocal(y), z ) == Blocks.water && temps[x][z] < 0.2D && y <= m_seaLevel)
					{
						cube.setBlock( x, Coords.blockToLocal(y), z, Blocks.ice, 0 );
					}
					//place snow
					else if( cube.getColumn().func_150810_a(x, y + 1, z) == Blocks.air  && temps[x][z] < 0.34D && cube.getColumn().func_150810_a(x, y - 1, z) != Blocks.water )
					{
						cube.getColumn().func_150807_a( x, y + 1, z, Blocks.snow_layer, 0 );
					}
				}
				catch (Exception e){
					System.out.printf( "x =  %d, z = %d", x , z );
					//throw new UnsupportedOperationException();
				}
				
			}
		}

		BlockFalling.field_149832_M = false;

		return true;
	}
}
