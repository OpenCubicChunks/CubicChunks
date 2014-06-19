/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuchaz.cubicChunks.generator.terrain;

import cuchaz.cubicChunks.generator.biome.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

/**
 *
 * @author Barteks2x
 */
public class AlternateTerrainProcessor extends CubeProcessor
{

	private int genToNormal[] =
	{
		0, 4, 8, 11, 15
	};//this is to avoid additional caches and almost duplicate methids. 1 block diffference shouldn't be noticable, players don't see temperature/rainfall.

	private final AlternateWorldColumnManager wcm;
	// these are the knobs for terrain generation 
	public static final double maxElev = 128; // approximately how high blocks will go
	public static final int CUBE_X_SIZE = 16;
	public static final int CUBE_Y_SIZE = 16;
	public static final int CUBE_Z_SIZE = 16;

	public static final int xNoiseSize = CUBE_X_SIZE / 4 + 1;
	public static final int yNoiseSize = CUBE_Y_SIZE / 8 + 1;
	public static final int zNoiseSize = CUBE_Z_SIZE / 4 + 1;

	private static BasicBuilder builderHigh;
	private static BasicBuilder builderLow;
	private static BasicBuilder builderAlpha;

	private static int seaLevel;
	private static final int octaves = 16;

	private final CubeWorldServer m_worldServer;
	private CubeBiomeGenBase[] m_biomes;

	private final Random m_rand;

	private final int maxSmoothDiameter = 9;
	private final int maxSmoothRadius = 4;

	private final double[][][] noiseArrayHigh;
	private final double[][][] noiseArrayLow;
	private final double[][][] noiseArrayAlpha;

	private double[][] noiseArrayHeight;
	private double[][] noiseArrayVolatility;
	private double[][] noiseArrayTemp;
	private double[][] noiseArrayRainfall;

	private final double[][][] rawTerrainArray;
	private final double[][][] terrainArray;

	private final double[] nearBiomeWeightArray;

	public AlternateTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );

		this.wcm = (AlternateWorldColumnManager)worldServer.getWorldChunkManager();
		this.m_worldServer = worldServer;
		this.m_biomes = null;

		this.m_rand = new Random( m_worldServer.getSeed() );

		seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();

		this.noiseArrayHeight = new double[xNoiseSize][zNoiseSize];
		this.noiseArrayVolatility = new double[xNoiseSize][zNoiseSize];

		this.noiseArrayHigh = new double[xNoiseSize][yNoiseSize][zNoiseSize];
		this.noiseArrayLow = new double[xNoiseSize][yNoiseSize][zNoiseSize];
		this.noiseArrayAlpha = new double[xNoiseSize][yNoiseSize][zNoiseSize];

		this.rawTerrainArray = new double[CUBE_X_SIZE][CUBE_Y_SIZE][CUBE_Z_SIZE];
		this.terrainArray = new double[CUBE_X_SIZE][CUBE_Y_SIZE][CUBE_Z_SIZE];

		this.nearBiomeWeightArray = new double[maxSmoothDiameter * maxSmoothDiameter];

		for( int x = -maxSmoothRadius; x <= maxSmoothRadius; x++ )
		{
			for( int z = -maxSmoothRadius; z <= maxSmoothRadius; z++ )
			{
				final double f1 = 10.0F / Math.sqrt( x * x + z * z + 0.2F );
				this.nearBiomeWeightArray[(x + maxSmoothRadius + (z + maxSmoothRadius) * maxSmoothDiameter)] = f1;
			}
		}

		double freqHor = Math.min( 1, 64 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 64 / maxElev ) / (4 * Math.PI);
		builderHigh = new BasicBuilder();
		builderHigh.setSeed( m_rand.nextInt() );
		builderHigh.setOctaves( octaves );
		builderHigh.setMaxElev( 1 );
		builderHigh.setSeaLevel( seaLevel / maxElev );
		builderHigh.setFreq( freqHor, freqVert, freqHor );
		builderHigh.build();

		builderLow = new BasicBuilder();
		builderLow.setSeed( m_rand.nextInt() );
		builderLow.setOctaves( octaves );
		builderLow.setMaxElev( 1 );
		builderLow.setSeaLevel( seaLevel / maxElev );
		builderLow.setFreq( freqHor, freqVert, freqHor );
		builderLow.build();

		builderAlpha = new BasicBuilder();
		builderAlpha.setSeed( m_rand.nextInt() );
		builderAlpha.setOctaves( octaves / 2 );
		builderAlpha.setMaxElev( 10 );
		builderAlpha.setFreq( freqHor, freqVert, freqHor );
		builderAlpha.build();

		/*double freq2d = 0.1 / (4 * Math.PI);

		 builderHeight = new BasicBuilder();
		 builderHeight.setSeed( m_rand.nextInt() );
		 builderHeight.setOctaves( octaves );
		 builderHeight.setMaxElev( 1 );
		 builderHeight.setScale( freq2d );
		 builderHeight.build();

		 builderVolatility = new BasicBuilder();
		 builderVolatility.setSeed( m_rand.nextInt() );
		 builderVolatility.setOctaves( octaves );
		 builderVolatility.setMaxElev( 1 );
		 builderVolatility.setScale( freq2d );
		 builderVolatility.build();*/
	}

	@Override
	public boolean calculate( Cube cube )
	{
		m_biomes = (CubeBiomeGenBase[])m_worldServer.getCubeWorldProvider().getWorldColumnMananger().getBiomesForGeneration(
			m_biomes,
			cube.getX() * 4 - maxSmoothRadius, cube.getZ() * 4 - maxSmoothRadius,
			xNoiseSize + maxSmoothDiameter, zNoiseSize + maxSmoothDiameter
		);

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

					if( val - yAbs > 0 )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.stone );
					}
					else if( yAbs < seaLevel )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.water );
					}
					else
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.air );
					}
				} // end yRel		
			} // end zRel
		} // end xRel
	}

	/**
	 * Generates noise arrays of size xNoiseSize * yNoiseSize * zNoiseSize
	 * 
	 * No issues with this. Tested by using size 16 (full resolution)
	 * 
	 * @param cubeX
	 * @param cubeY
	 * @param cubeZ
	 * @return
	 */
	private void generateNoiseArrays( Cube cube )
	{
		int cubeXMin = cube.getX() * (xNoiseSize - 1);
		int cubeYMin = cube.getY() * (yNoiseSize - 1);
		int cubeZMin = cube.getZ() * (zNoiseSize - 1);

		this.noiseArrayHeight = wcm.getHeightArray( cube.getX(), cube.getZ() );
		this.noiseArrayVolatility = wcm.getVolArray( cube.getX(), cube.getZ() );

		this.noiseArrayRainfall = wcm.getRainfallArray( cube.getX(), cube.getZ() );
		this.noiseArrayTemp = wcm.getTempArray( cube.getX(), cube.getZ() );
		for( int x = 0; x < xNoiseSize; x++ )
		{
			int xPos = cubeXMin + x;

			for( int z = 0; z < zNoiseSize; z++ )
			{
				int zPos = cubeZMin + z;

				for( int y = 0; y < yNoiseSize; y++ )
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
		for( int x = 0; x < xNoiseSize; x++ )
		{
			for( int z = 0; z < zNoiseSize; z++ )
			{
				double height = noiseArrayHeight[genToNormal[x]][genToNormal[z]];
				double vol = noiseArrayVolatility[genToNormal[x]][genToNormal[z]];
				double temp = noiseArrayTemp[genToNormal[x]][genToNormal[z]];
				double rainfall = noiseArrayRainfall[genToNormal[x]][genToNormal[z]];
				//height *= 1 - vol;

				rainfall *= temp;

				vol = Math.abs( vol );
				vol *= Math.max( 0, height );
				vol *= 1 - Math.pow( 1 - rainfall, 4 );
				vol = 0.95 * vol + 0.05;//It should be done here
				
				if( height < 0 )
				{
					height *= 0.987;
					height = -Math.pow( -height, MathHelper.clamp_double( -height * 4, 2, 4 ) );
				}

				for( int y = 0; y < yNoiseSize; y++ )
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
						output = output * (1.0D - a) - 10.0D * a;
					}

					this.rawTerrainArray[x][y][z] = output;
				}
			}
		}
	}

	private void amplifyNoiseArray()
	{
		for( int x = 0; x < xNoiseSize; x++ )
		{
			for( int z = 0; z < zNoiseSize; z++ )
			{
				for( int y = 0; y < yNoiseSize; y++ )
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
		int xSteps = 16 / (xNoiseSize - 1);
		int ySteps = 16 / (yNoiseSize - 1);
		int zSteps = 16 / (zNoiseSize - 1);

		// use the noise to generate the terrain
		for( int noiseX = 0; noiseX < xNoiseSize - 1; noiseX++ )
		{
			for( int noiseZ = 0; noiseZ < zNoiseSize - 1; noiseZ++ )
			{
				for( int noiseY = 0; noiseY < yNoiseSize - 1; noiseY++ )
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
