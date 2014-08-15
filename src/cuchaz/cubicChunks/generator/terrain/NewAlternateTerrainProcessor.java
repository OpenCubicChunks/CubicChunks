/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuchaz.cubicChunks.generator.terrain;

import cuchaz.cubicChunks.generator.biome.alternateGen.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.*;

/**
 *
 * @author Barteks2x
 */
public class NewAlternateTerrainProcessor extends CubeProcessor
{

	private int genToNormal[] =
	{
		0, 4, 8, 10, 16
	};

	private final AlternateWorldColumnManager wcm;
	// these are the knobs for terrain generation 

	private static BasicBuilder builderHigh;
	private static BasicBuilder builderLow;
	private static BasicBuilder builderAlpha;

	private static int seaLevel;
	private static final int octaves = 16;

	private final CubeWorldServer m_worldServer;
	private CubeBiomeGenBase[] m_biomes;

	private final Random m_rand;

	private final double[][][] noiseArrayHigh;
	private final double[][][] noiseArrayLow;
	private final double[][][] noiseArrayAlpha;

	private double[][] noiseArrayHeight;
	private double[][] noiseArrayVolatility;
	private double[][] noiseArrayTemp;
	private double[][] noiseArrayRainfall;

	private final double[][][] rawTerrainArray;
	private final double[][][] terrainArray;

	public NewAlternateTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
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

		double freqHor = Math.min( 1, 64 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 64 / maxElev ) / (4 * Math.PI);
		builderHigh = new BasicBuilder();
		builderHigh.setSeed( m_rand.nextInt() );
		builderHigh.setOctaves( octaves );
		builderHigh.setMaxElev( 150 );
		builderHigh.setSeaLevel( seaLevel/maxElev );
		//builderHigh.setFreq( freqHor, freqVert, freqHor );
		builderHigh.setFreq(0.011731323D);
		//builderHigh.setClamp( -5, 5 );
		builderHigh.setlacunarity(2);
		builderHigh.build();

		builderLow = new BasicBuilder();
		builderLow.setSeed( m_rand.nextInt() );
		builderLow.setOctaves( octaves );
		builderLow.setMaxElev( 150 );
		builderLow.setSeaLevel( seaLevel/maxElev );
		//builderLow.setFreq( freqHor, freqVert, freqHor );
		builderLow.setFreq(0.011731323D);
		//builderLow.setClamp( -5, 5 );
		builderLow.setlacunarity(2);
		builderLow.build();

		builderAlpha = new BasicBuilder();
		builderAlpha.setSeed( m_rand.nextInt() );
		builderAlpha.setOctaves( octaves / 2 );
		builderAlpha.setMaxElev( 5 );
		//builderAlpha.setFreq( freqHor, freqVert, freqHor );
		builderAlpha.setFreq(0.066837109D);
		builderAlpha.setSeaLevel( 0.5D );
		builderAlpha.setClamp( 0, 1 );
		builderAlpha.setlacunarity(2);
		builderAlpha.build();
	}

	@Override
	public boolean calculate( Cube cube )
	{
		CubeWorldServer world = (CubeWorldServer)cube.getWorld();
		AlternateWorldColumnManager wcm = (AlternateWorldColumnManager)world.getWorldChunkManager();
		/*double [][] height = wcm.getHeightArray(cube.getX(), cube.getZ());
		//double [][] volatility = wcm.getVolArray(cube.getX(), cube.getZ());
		//double [][] temp = wcm.getTempArray(cube.getX(), cube.getZ());
		//double [][] hum = wcm.getRainfallArray(cube.getX(), cube.getZ());
		int heightMax = -1999;
		int heightMin = 1999;
		double volMax = 0;
		for (int i = 0; i < 16; i++){
			for (int j = 0; j < 16; j++){
				int height1 = (int)wcm.getRealHeight(height[i][j]);
				//double volatility1 = wcm.getRealVolatility(volatility[i][j], height[i][j], hum[i][j], temp[i][j]);
				heightMax = heightMax > height1 ? heightMax : height1 ;
				heightMin = heightMin < height1 ? heightMin : height1 ;
				//volMax = volMax > volatility1 ? volMax : volatility1 ;
			}
		}
		int buffer = 10;// +/-3 cubes
		if ( cube.getY() * 2 < heightMin - buffer){return setToStone(cube);}
		if ( cube.getY() * 2 > heightMax + buffer){return setToAir(cube);}*/
		return calculateSurface( cube );
	}
	
	public boolean setToStone( Cube cube)
	{
		for (int i = 0;i < 16; i++){
			for (int j = 0; j < 16; j++){
				for (int k = 0; k < 16; k++){
					cube.setBlockForGeneration(i, j, k, Blocks.stone);
				}
			}
		}
		return true;
	}
	
	public boolean setToAir( Cube cube)
	{
		for (int j = 0; j < 16; j++){
			if (Coords.localToBlock( cube.getY(), j ) > seaLevel){
				for (int i = 0; i < 16; i++){
					for (int k = 0; k < 16; k++){
						cube.setBlockForGeneration(i, j, k, Blocks.air);
					}
				}
			}
			else {
				for (int i = 0; i < 16; i++){
					for (int k= 0; k < 16; k++){
						cube.setBlockForGeneration(i, j, k, Blocks.water);
					}
				}
			}
		}
		return true;
	}
	
	
	
	public boolean calculateSurface( Cube cube )
	{

		this.generateNoiseArrays( cube );

		this.generateTerrainArray( cube );

		//this.amplifyNoiseArray();

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
				vol = this.wcm.getRealVolatility( vol, height, rainfall, temp ) * 1.4 + 0.04D;
				height = this.wcm.getRealHeight( height ) * (50D + 250D * vol)  ;
				double output;
				double d8;
				int yPos = cube.getY() * (yNoiseSize - 1);
                for (int y = 0; y < yNoiseSize; y++)
                {
                    d8 = (yPos + y - height)  / vol / 4 * 3;
                    if (d8 < 0.0D)
                    {
                        d8 *= 4.0D;
                    }

                    final double vol1Low = this.noiseArrayLow[x][y][z] /* biomeConfig.volatility1*/;
                    final double vol2High = this.noiseArrayHigh[x][y][z] /* biomeConfig.volatility2*/;
                    final double noiseAlpha = this.noiseArrayAlpha[x][y][z];
                    output = vol1Low + (vol2High - vol1Low) * noiseAlpha;
                    output -= d8;
                    this.rawTerrainArray[x][y][z] = output;                
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
