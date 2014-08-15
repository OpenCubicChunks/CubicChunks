package cuchaz.cubicChunks.generator.terrain;

import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.*;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_X;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Y;
import static cuchaz.cubicChunks.util.Coords.CUBE_MAX_Z;
import net.minecraft.init.Blocks;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;

public abstract class AbstractTerrainProcessor3dNoise extends CubeProcessor{

	protected final double[][][] noiseArrayHigh;
	protected final double[][][] noiseArrayLow;
	protected final double[][][] noiseArrayAlpha;

	protected final double[][][] rawTerrainArray;
	protected final double[][][] terrainArray;
	
	protected final IBuilder builderHigh;
	protected final IBuilder builderLow;
	protected final IBuilder builderAlpha;
	
	protected final int seaLevel;
	
	protected boolean amplify;
	
	protected final CubeWorldServer m_worldServer;
	
	public AbstractTerrainProcessor3dNoise(String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );
		
		this.m_worldServer = worldServer;
		
		this.noiseArrayHigh = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];
		this.noiseArrayLow = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];
		this.noiseArrayAlpha = new double[X_NOISE_SIZE][Y_NOISE_SIZE][Z_NOISE_SIZE];

		this.rawTerrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
		this.terrainArray = new double[CUBE_MAX_X][CUBE_MAX_Y][CUBE_MAX_Z];
		
		this.builderHigh = createHighBuilder();
		this.builderLow = createLowBuilder();
		this.builderAlpha = createAlphaBuilder();
		
		this.amplify = true;
		this.seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();
	}
	
	protected abstract IBuilder createHighBuilder();
	protected abstract IBuilder createLowBuilder();
	protected abstract IBuilder createAlphaBuilder();
	
	@Override
	public boolean calculate( Cube cube )
	{
		this.generateNoiseArrays( cube );

		this.generateTerrainArray( cube );

		if(amplify)
			this.amplifyNoiseArray();

		this.expandNoiseArray();

		this.generateTerrain( cube );

		return true;
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
	
	protected abstract void generateTerrainArray(Cube cube);
	
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

	protected double lerp( double a, double min, double max )
	{
		return min + a * (max - min);
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
						val - yAbs > 0 ? Blocks.stone : yAbs <= seaLevel ? Blocks.water : Blocks.air );
				} // end yRel		
			} // end zRel
		} // end xRel
	}
}
