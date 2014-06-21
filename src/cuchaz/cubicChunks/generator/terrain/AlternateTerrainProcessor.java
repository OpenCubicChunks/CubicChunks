/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuchaz.cubicChunks.generator.terrain;

import cuchaz.cubicChunks.generator.biome.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import static cuchaz.cubicChunks.util.Coords.*;
import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.*;

/**
 *
 * @author Barteks2x
 */
public class AlternateTerrainProcessor extends CubeProcessor
{
	private static final int genToNormal[] =
	{
		0, 4, 8, 12, 16
	};

	private final AlternateWorldColumnManager wcm;

	private final BasicBuilder builderHigh;
	private final BasicBuilder builderLow;
	private final BasicBuilder builderAlpha;

	private final int seaLevel;
	private final int octaves = 16;

	private final CubeWorldServer m_worldServer;
	//private CubeBiomeGenBase[] m_biomes;

	private final Random rand;

	private final double[][][] noiseArrayHigh;
	private final double[][][] noiseArrayLow;
	private final double[][][] noiseArrayAlpha;

	private double[][] noiseArrayHeight;
	private double[][] noiseArrayVolatility;
	private double[][] noiseArrayTemp;
	private double[][] noiseArrayRainfall;

	private final double[][][] rawTerrainArray;
	private final double[][][] terrainArray;

	public AlternateTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );

		this.wcm = (AlternateWorldColumnManager)worldServer.getWorldChunkManager();
		this.m_worldServer = worldServer;

		this.rand = new Random( m_worldServer.getSeed() );

		seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();

		this.noiseArrayHeight = new double[X_NOISE_SIZE][Z_NOISE_SIZE];
		this.noiseArrayVolatility = new double[X_NOISE_SIZE][Z_NOISE_SIZE];

		this.noiseArrayHigh = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];
		this.noiseArrayLow = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];
		this.noiseArrayAlpha = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];

		this.rawTerrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
		this.terrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];

		double freqHor = Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 128 / maxElev ) / (4 * Math.PI);

		builderHigh = new BasicBuilder();
		builderHigh.setSeed( rand.nextInt() );
		builderHigh.setOctaves( octaves );
		builderHigh.setMaxElev( 1 );
		builderHigh.setSeaLevel( seaLevel / maxElev );
		builderHigh.setFreq( freqHor, freqVert, freqHor );
		builderHigh.build();

		builderLow = new BasicBuilder();
		builderLow.setSeed( rand.nextInt() );
		builderLow.setOctaves( octaves );
		builderLow.setMaxElev( 1 );
		builderLow.setSeaLevel( seaLevel / maxElev );
		builderLow.setFreq( freqHor, freqVert, freqHor );
		builderLow.build();

		builderAlpha = new BasicBuilder();
		builderAlpha.setSeed( rand.nextInt() );
		builderAlpha.setOctaves( octaves / 2 );
		builderAlpha.setMaxElev( 10 );
		builderAlpha.setFreq( freqHor, freqVert, freqHor );
		builderAlpha.build();
	}

	@Override
	public boolean calculate( Cube cube )
	{
		this.generateNoiseArrays( cube );

		this.generateTerrainArray( cube );

		this.amplifyNoiseArray();

		this.expandNoiseArray();

		this.generateTerrain( cube );

		return true;
	}

	protected void generateTerrain( Cube cube )
	{
		int cubeY = cube.getY();

		for( int xRel = 0; xRel < 16; xRel++ )
		{
			for( int zRel = 0; zRel < 16; zRel++ )
			{
				for( int yRel = 0; yRel < 16; yRel++ )
				{
					double val = terrainArray[xRel][yRel][zRel];

					int yAbs = Coords.localToBlock( cubeY, yRel );

					cube.setBlockForGeneration( xRel, yRel, zRel,
						val - yAbs > 0 ? Blocks.stone : yAbs < seaLevel ? Blocks.water : Blocks.air );
				} // end yRel		
			} // end zRel
		} // end xRel
	}

	/**
	 * Generates noise arrays of size X_NOISE_SIZE * Y_NOISE_SIZE * Z_NOISE_SIZE
 
	 No issues with this. Tested by using size 16 (full resolution)
	 * 
	 * @param cubeX
	 * @param cubeY
	 * @param cubeZ
	 * @return
	 */
	private void generateNoiseArrays( Cube cube )
	{
		int cubeXMin = cube.getX() * (X_NOISE_SIZE - 1);
		int cubeYMin = cube.getY() * (Y_NOISE_SIZE - 1);
		int cubeZMin = cube.getZ() * (Z_NOISE_SIZE - 1);

		this.noiseArrayHeight = wcm.getHeightArray( cube.getX(), cube.getZ() );
		this.noiseArrayVolatility = wcm.getVolArray( cube.getX(), cube.getZ() );

		this.noiseArrayRainfall = wcm.getRainfallArray( cube.getX(), cube.getZ() );
		this.noiseArrayTemp = wcm.getTempArray( cube.getX(), cube.getZ() );

		for( int x = 0; x < X_NOISE_SIZE; x++ )
		{
			int xPos = cubeXMin + x;

			for( int z = 0; z < Z_NOISE_SIZE; z++ )
			{
				int zPos = cubeZMin + z;

				for( int y = 0; y < Y_NOISE_SIZE; y++ )
				{
					int yPos = cubeYMin + y;

					this.noiseArrayHigh[x][y][z] = builderHigh.getValue( xPos, yPos, zPos );
					this.noiseArrayLow[x][y][z] = builderLow.getValue( xPos, yPos, zPos );
					this.noiseArrayAlpha[x][y][z] = builderAlpha.getValue( xPos, yPos, zPos );

				}
			}
		}
	}

	private void generateTerrainArray( Cube cube )
	{
		for( int x = 0; x < X_NOISE_SIZE; x++ )
		{
			for( int z = 0; z < Z_NOISE_SIZE; z++ )
			{
				double height = noiseArrayHeight[genToNormal[x]][genToNormal[z]];
				double vol = noiseArrayVolatility[genToNormal[x]][genToNormal[z]];
				double temp = noiseArrayTemp[genToNormal[x]][genToNormal[z]];
				double rainfall = noiseArrayRainfall[genToNormal[x]][genToNormal[z]];

				vol = this.wcm.getRealVolatility( vol, height, rainfall, temp );
				height = this.wcm.getRealHeight( height );
				rainfall *= temp;

				for( int y = 0; y < Y_NOISE_SIZE; y++ )
				{
					double output;

					double vol1Low = MathHelper.clamp_double( this.noiseArrayLow[x][y][z], -1, 1 ) /*/ 512.0D * 0.5 biomeConfig.volatility1*/;
					double vol2High = MathHelper.clamp_double( this.noiseArrayHigh[x][y][z], -1, 1 )/*/ 512.0D  * 0.5 biomeConfig.volatility2*/;

					final double noiseAlpha = this.noiseArrayAlpha[x][y][z] * 10;

					if( noiseAlpha < 0 /*biomeConfig.volatilityWeight1*/ )
					{
						output = vol1Low;
					}
					else if( noiseAlpha > 1/*biomeConfig.volatilityWeight2*/ )
					{
						output = vol2High;
					}
					else
					{
						output = lerp( noiseAlpha, vol1Low, vol2High );
					}

					//make height range lower
					output *= vol;
					//height shift
					output += height;

					double maxYSections = maxElev / 2;
					int yAbs = cube.getY() * 8 + y;
					if( yAbs > maxYSections - 4 )
					{
						final double a = (yAbs - (maxYSections - 4)) / 3.0F;
						output = output * (1.0D - a) - a;
					}

					this.rawTerrainArray[x][y][z] = output;
				}
			}
		}
	}

	private void amplifyNoiseArray()
	{
		for( int x = 0; x < X_NOISE_SIZE; x++ )
		{
			for( int z = 0; z < Z_NOISE_SIZE; z++ )
			{
				for( int y = 0; y < Y_NOISE_SIZE; y++ )
				{
					this.rawTerrainArray[x][y][z] *= maxElev;
				}
			}
		}
	}

	/**
	 * expand the noise array to 16x16x16 by interpolating the values.
	 * 
	 * @param arrayIn
	 * @return
	 */
	private void expandNoiseArray()
	{
		int xSteps = 16 / (X_NOISE_SIZE - 1);
		int ySteps = 16 / (Y_NOISE_SIZE - 1);
		int zSteps = 16 / (Z_NOISE_SIZE - 1);

		// use the noise to generate the terrain
		for( int noiseX = 0; noiseX < X_NOISE_SIZE - 1; noiseX++ )
		{
			for( int noiseZ = 0; noiseZ < Z_NOISE_SIZE - 1; noiseZ++ )
			{
				for( int noiseY = 0; noiseY < Y_NOISE_SIZE - 1; noiseY++ )
				{
					// get the noise samples
					double x0y0z0 = rawTerrainArray[noiseX][noiseY][noiseZ];
					double x0y0z1 = rawTerrainArray[noiseX][noiseY][noiseZ + 1];
					double x1y0z0 = rawTerrainArray[noiseX + 1][noiseY][noiseZ];
					double x1y0z1 = rawTerrainArray[noiseX + 1][noiseY][noiseZ + 1];

					double x0y1z0 = rawTerrainArray[noiseX][noiseY + 1][noiseZ];
					double x0y1z1 = rawTerrainArray[noiseX][noiseY + 1][noiseZ + 1];
					double x1y1z0 = rawTerrainArray[noiseX + 1][noiseY + 1][noiseZ];
					double x1y1z1 = rawTerrainArray[noiseX + 1][noiseY + 1][noiseZ + 1];

					for( int x = 0; x < xSteps; x++ )
					{
						int xRel = noiseX * xSteps + x;

						double xd = (double)x / xSteps;

						//interpolate along x
						double xy0z0 = lerp( xd, x0y0z0, x1y0z0 );
						double xy0z1 = lerp( xd, x0y0z1, x1y0z1 );
						double xy1z0 = lerp( xd, x0y1z0, x1y1z0 );
						double xy1z1 = lerp( xd, x0y1z1, x1y1z1 );

						for( int z = 0; z < zSteps; z++ )
						{
							int zRel = noiseZ * zSteps + z;

							double zd = (double)z / zSteps;

							//interpolate along z
							double xy0z = lerp( zd, xy0z0, xy0z1 );
							double xy1z = lerp( zd, xy1z0, xy1z1 );

							for( int y = 0; y < ySteps; y++ )
							{
								int yRel = noiseY * ySteps + y;

								double yd = (double)y / ySteps;

								// interpolate along y
								double xyz = lerp( yd, xy0z, xy1z );

								terrainArray[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}
	}

	private double lerp( double a, double min, double max )
	{
		return min + a * (max - min);
	}
}
