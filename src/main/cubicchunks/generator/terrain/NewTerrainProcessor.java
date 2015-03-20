/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.generator.terrain;

import java.util.Random;

import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
import cubicchunks.generator.builder.BasicBuilder;
import cubicchunks.generator.builder.ComplexWorldBuilder;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeProcessor;
import cubicchunks.world.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldType;

public class NewTerrainProcessor extends CubeProcessor
{
	private static final int CUBE_X_SIZE = 16;
	private static final int CUBE_Y_SIZE = 16;
	private static final int CUBE_Z_SIZE = 16;
	
    private static final int xNoiseSize = CUBE_X_SIZE / 4 + 1;
    private static final int yNoiseSize = CUBE_Y_SIZE / 8 + 1;
    private static final int zNoiseSize = CUBE_Z_SIZE / 4 + 1;
    
    // Water level at lower resolution
    private final int[] waterLevelRaw = new int[25];
    // Water level for each column
    private final byte[] waterLevel = new byte[CUBE_X_SIZE * CUBE_Z_SIZE];
	
	private CubeWorldServer m_worldServer;
	private CubeBiomeGenBase[] m_biomes;
	
	private Random m_rand;
	
    private double riverVol;
    private double riverHeight;
    // Always false if improved rivers disabled
    private boolean riverFound = false;

    private double volatilityFactor;
    private double heightFactor;

    private final int heightCap;

    private final int maxSmoothDiameter = 9;
    private final int maxSmoothRadius = 4;
	
	private double[][][] noiseArrayHigh;
	private double[][][] noiseArrayLow;
	private double[][][] noiseArrayAlpha;
	
	private double[][] noiseArrayHeight;
	
	private double[][][] rawTerrainArray;
	private double[][][] terrainArray;
	
	private double[] nearBiomeWeightArray;
	
	private static BasicBuilder builderHigh;
	private static BasicBuilder builderLow;
	private static BasicBuilder builderAlpha;
	private static BasicBuilder builderHeight;
	
//	private static ComplexWorldBuilder builder;
	private static int seaLevel;
	
	// these are the knobs for terrain generation
	private static int maxElev = 800; // approximately how high blocks will go
	private static int amplify = 30000; // amplify factor for the noise array.
	private static int octaves = 14; // size of features. increasing by 1 approximately doubles the size of features.
	
	public NewTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );
		
		this.m_worldServer = worldServer;
		this.m_biomes = null;
		
		this.m_rand = new Random( m_worldServer.getSeed() );
		
		seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();
		
		this.heightCap = 16;
		
		this.noiseArrayHeight = new double[xNoiseSize][zNoiseSize];
		
		this.noiseArrayHigh = new double[xNoiseSize][yNoiseSize][zNoiseSize];
		this.noiseArrayLow = new double[xNoiseSize][yNoiseSize][zNoiseSize];
		this.noiseArrayAlpha = new double[xNoiseSize][yNoiseSize][zNoiseSize];
		
		this.rawTerrainArray = new double[CUBE_X_SIZE][CUBE_Y_SIZE][CUBE_Z_SIZE];
		this.terrainArray = new double[CUBE_X_SIZE][CUBE_Y_SIZE][CUBE_Z_SIZE];
		
        this.nearBiomeWeightArray = new double[maxSmoothDiameter * maxSmoothDiameter];

        for (int x = -maxSmoothRadius; x <= maxSmoothRadius; x++)
        {
            for (int z = -maxSmoothRadius; z <= maxSmoothRadius; z++)
            {
                final double f1 = 10.0F / Math.sqrt(x * x + z * z + 0.2F);
                this.nearBiomeWeightArray[(x + maxSmoothRadius + (z + maxSmoothRadius) * maxSmoothDiameter)] = f1;
            }
        }
		
		builderHigh = new BasicBuilder();
		builderHigh.setSeed(m_rand.nextInt());
		builderHigh.setOctaves(octaves);
		builderHigh.setMaxElev(maxElev);
		builderHigh.build();
		
		builderLow = new BasicBuilder();
		builderLow.setSeed(m_rand.nextInt());
		builderLow.setOctaves(octaves);
		builderLow.setMaxElev(maxElev);
		builderLow.build();
		
		builderAlpha = new BasicBuilder();
		builderAlpha.setSeed(m_rand.nextInt());
		builderAlpha.setOctaves(8);
		builderAlpha.setMaxElev(maxElev);
		builderAlpha.build();
		
		builderHeight = new BasicBuilder();
		builderHeight.setSeed(m_rand.nextInt());
		builderHeight.setOctaves(8);
		builderHeight.setMaxElev(1);
		builderHeight.build();
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
		
		double maxNoise = -9000;
		double minNoise = 9000;
		
		for( int xRel=0; xRel < 16; xRel++ )
		{
			for( int zRel=0; zRel < 16; zRel++ )
			{				
				for( int yRel=0; yRel < 16; yRel++ )
				{
					double val = terrainArray[xRel][yRel][zRel];
					
//					System.out.println(xRel + ":" + yRel + ":" + zRel + ":" + val);
					
					if(val > maxNoise) maxNoise = val;
					if(val < minNoise) minNoise = val;
					
					int yAbs = Coords.localToBlock(cubeY, yRel);
					
					if( val - yAbs > 0 )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.stone);
					}
					else if( yAbs < seaLevel )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.water);
					}
					else
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.air);
					}
				} // end yRel		
			} // end zRel
		} // end xRel
		
//		System.out.println("maxNoise: " + maxNoise);
//		System.out.println("minNoise: " + minNoise);
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
	private void generateNoiseArrays(Cube cube)
	{		
		int cubeXMin = cube.getX() * (xNoiseSize - 1);
		int cubeYMin = cube.getY() * (yNoiseSize - 1);
		int cubeZMin = cube.getZ() * (zNoiseSize - 1);
		
		for (int x = 0; x < xNoiseSize; x++)
		{
			int xPos = cubeXMin + x;
			
			for (int z = 0; z < zNoiseSize; z++)
			{
				int zPos = cubeZMin + z;
				
				this.noiseArrayHeight[x][z] = builderHeight.getValue(xPos, 0, zPos);
				
				for (int y = 0; y < yNoiseSize; y++)
				{
					int yPos = cubeYMin + y;
					
					this.noiseArrayHigh[x][y][z] = builderHigh.getValue(xPos, yPos, zPos);
					this.noiseArrayLow[x][y][z] = builderLow.getValue(xPos, yPos, zPos);
					this.noiseArrayAlpha[x][y][z] = builderAlpha.getValue(xPos, yPos, zPos);
					
				}
			}
		}
	}
	
	private void generateTerrainArray(Cube cube)
	{				
		for (int x = 0; x < xNoiseSize; x++)
        {
            for (int z = 0; z < zNoiseSize; z++)
            {
                double noiseHeight = noiseArrayHeight[x][z];
                
                if (noiseHeight < 0.0D)
                {
                    noiseHeight = -noiseHeight * 0.3D;
                }
                
                noiseHeight = noiseHeight * 3.0D - 2.0D;

                if (noiseHeight < 0.0D)
                {
                    noiseHeight /= 2.0D;
                    
                    if (noiseHeight < -1.0D)
                    {
                        noiseHeight = -1.0D;
                    }
//                    noiseHeight -= biomeConfig.maxAverageDepth;
                    noiseHeight /= 1.4D;
                    noiseHeight /= 2.0D;

                } 
                else
                {
                    if (noiseHeight > 1.0D)
                    {
                    	System.out.println("noiseHeight >= 1.0D; ");
                        noiseHeight = 1.0D;
                    }
//                    noiseHeight += biomeConfig.maxAverageHeight;
                    noiseHeight /= 8.0D;
                }

//                if (!worldConfig.oldTerrainGenerator)
//                {
//                    if (worldConfig.improvedRivers)
//                        this.biomeFactorWithRivers(x, z, usedYSections, noiseHeight);
//                    else
                        this.biomeFactor(x, z, noiseHeight);
//                } else
//                    this.oldBiomeFactor(x, z, i2D, usedYSections, noiseHeight);

//                i2D++;

                for (int y = 0; y < yNoiseSize; y++)
                {
                    double output;
                    double d8;

                    if (this.riverFound)
                    {
                        d8 = (this.riverHeight - y) * 12.0D * 128.0D / this.heightCap / this.riverVol;
                    } else
                    {
                        d8 = (this.heightFactor - y) * 12.0D * 128.0D / this.heightCap / this.volatilityFactor;
                    }

                    if (d8 > 0.0D)
                    {
                        d8 *= 4.0D;
                    }

                    final double vol1Low = this.noiseArrayLow[x][y][z] / 512.0D * 0.5/* biomeConfig.volatility1*/;
                    final double vol2High = this.noiseArrayHigh[x][y][z] / 512.0D  * 0.5/* biomeConfig.volatility2*/;

                    final double noiseAlpha = (this.noiseArrayAlpha[x][y][z] / 10.0D + 1.0D) / 2.0D;
                    
                    if (noiseAlpha < 0.0/*biomeConfig.volatilityWeight1*/)
                    {
                        output = vol1Low;
                    } else if (noiseAlpha > 1.0/*biomeConfig.volatilityWeight2*/)
                    {
                        output = vol2High;
                    } else
                    {
                        output = vol1Low + (vol2High - vol1Low) * noiseAlpha;
                    }

//                    if (!biomeConfig.disableNotchHeightControl)
//                    {
//                        output += d8;
//
//                        if (y > maxYSections - 4)
//                        {
//                            final double d12 = (y - (maxYSections - 4)) / 3.0F;
//                            // Reduce last three layers
//                            output = output * (1.0D - d12) + -10.0D * d12;
//                        }
//
//                    }
//                    if (this.riverFound)
//                    {
//                        output += biomeConfig.riverHeightMatrix[y];
//                    } else
//                    {
//                        output += biomeConfig.heightMatrix[y];
//                    }

                    this.rawTerrainArray[x][y][z] = output;                
                }
            }
        }
	}
	
	private void amplifyNoiseArray()
	{
		for(int x = 0; x < xNoiseSize; x++)
		{
			for (int z = 0; z < zNoiseSize; z++)
			{
				for (int y = 0; y < yNoiseSize; y++)
				{
					this.rawTerrainArray[x][y][z] *= amplify;
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
		for( int noiseX=0; noiseX < xNoiseSize - 1; noiseX++ )
		{
			for( int noiseZ=0; noiseZ < zNoiseSize - 1; noiseZ++ )
			{
				for( int noiseY=0; noiseY < yNoiseSize - 1; noiseY++ )
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
					
					for (int x = 0; x < xSteps; x++)
					{
						int xRel = noiseX * xSteps + x;
						
						double xd = (double) x / xSteps;
						
						//interpolate along x
						double xy0z0 = lerp(xd, x0y0z0, x1y0z0);
						double xy0z1 = lerp(xd, x0y0z1, x1y0z1);
						double xy1z0 = lerp(xd, x0y1z0, x1y1z0);
						double xy1z1 = lerp(xd, x0y1z1, x1y1z1);
						
						for (int z = 0; z < zSteps; z++)
						{
							int zRel = noiseZ * zSteps + z;
							
							double zd = (double) z / zSteps;
							
							//interpolate along z
							double xy0z = lerp(zd, xy0z0, xy0z1);
							double xy1z = lerp(zd, xy1z0, xy1z1);
							
							for (int y = 0; y < ySteps; y++)
							{
								int yRel = noiseY * ySteps + y;
								
								double yd = (double) y / ySteps;
								
								// interpolate along y
								double xyz = lerp(yd, xy0z, xy1z);
								
								terrainArray[xRel][yRel][zRel] = xyz;
							}
						}
					}
				}
			}
		}
	}
	
    private void biomeFactor(int x, int z, double noiseHeight)
    {
        float volatilitySum = 0.0F;
        double heightSum = 0.0F;
        float biomeWeightSum = 0.0F;

        final CubeBiomeGenBase centerBiomeConfig = this.m_biomes[(x + this.maxSmoothRadius + (z + this.maxSmoothRadius)
                * (xNoiseSize + this.maxSmoothDiameter))];
        final int lookRadius = 2/*centerBiomeConfig.smoothRadius*/;

        float nextBiomeHeight;
		double biomeWeight;

        for (int nextX = -lookRadius; nextX <= lookRadius; nextX++)
        {
            for (int nextZ = -lookRadius; nextZ <= lookRadius; nextZ++)
            {
                final CubeBiomeGenBase nextBiomeConfig = this.m_biomes[(x + nextX + this.maxSmoothRadius + (z + nextZ + this.maxSmoothRadius)
                        * (xNoiseSize + this.maxSmoothDiameter))];

                nextBiomeHeight = nextBiomeConfig.biomeHeight;

                biomeWeight = this.nearBiomeWeightArray[(nextX + this.maxSmoothRadius + (nextZ + this.maxSmoothRadius)
                        * this.maxSmoothDiameter)]
                        / (nextBiomeHeight + 2.0F);
                biomeWeight = Math.abs(biomeWeight);
                if (nextBiomeHeight > centerBiomeConfig.biomeHeight)
                {
                    biomeWeight /= 2.0F;
                }
                volatilitySum += nextBiomeConfig.biomeVolatility * biomeWeight;
                heightSum += nextBiomeHeight * biomeWeight;
                biomeWeightSum += biomeWeight;
            }
        }

        volatilitySum /= biomeWeightSum;
        heightSum /= biomeWeightSum;

        this.waterLevelRaw[x * xNoiseSize + z] = seaLevel; // (byte) centerBiomeConfig.waterLevelMax;

        volatilitySum = volatilitySum * 0.9F + 0.1F;   // Must be != 0
        heightSum = (heightSum * 4.0F - 1.0F) / 8.0F;  // Silly magic numbers

        this.volatilityFactor = volatilitySum;
        this.heightFactor = 16 * (2.0D + heightSum + noiseHeight * 0.2D) / 4.0D;
    }
	
	private double lerp(double a, double min, double max)
	{
		return min + a * (max - min);
	}
}
