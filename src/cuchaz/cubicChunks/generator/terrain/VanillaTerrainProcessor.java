/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 *     Nick Whitney - oh so much. Restructuring of terrain gen and biome gen,
 *     		for a start.
 ******************************************************************************/
package cuchaz.cubicChunks.generator.terrain;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldType;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.noise.NoiseGeneratorOctaves;
import cuchaz.cubicChunks.generator.noise.NoiseGeneratorPerlin;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.world.Cube;

public class VanillaTerrainProcessor extends CubeProcessor
{
	private CubeWorldServer m_worldServer;
	private CubeBiomeGenBase[] m_biomes;
	
	/** RNG. */
    private Random rand;
    private NoiseGeneratorOctaves noiseGen1;
    private NoiseGeneratorOctaves noiseGen2;
    private NoiseGeneratorOctaves noiseGen3;
    private NoiseGeneratorPerlin noiseGen4;
    public NoiseGeneratorOctaves noiseGen5;
    public NoiseGeneratorOctaves noiseGen6;
    public NoiseGeneratorOctaves mobSpawnerNoise;

    private WorldType terrainType;
    
    private final double[] rawTerrain;
    
    private final float[] parabolicField;
    
    private double[] stoneNoise = new double[256];

    /** The biomes that are used to generate the chunk */
    private CubeBiomeGenBase[] biomesForGeneration;
    
    double[] noise3;
    double[] noise1;
    double[] noise2;
    double[] noise6;
    
    int[][] field_73219_j = new int[32][32];
	
	public VanillaTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer.getCubeProvider(), batchSize );
		
		m_worldServer = worldServer;
		m_biomes = null;
		
        this.terrainType = m_worldServer.getWorldInfo().getTerrainType();
        
        this.rand = new Random( m_worldServer.getSeed());
        
        this.noiseGen1 = new NoiseGeneratorOctaves(this.rand, 16);
        this.noiseGen2 = new NoiseGeneratorOctaves(this.rand, 16);
        this.noiseGen3 = new NoiseGeneratorOctaves(this.rand, 8);
        this.noiseGen4 = new NoiseGeneratorPerlin(this.rand, 4);
        this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 10);
        this.noiseGen6 = new NoiseGeneratorOctaves(this.rand, 16);
        this.mobSpawnerNoise = new NoiseGeneratorOctaves(this.rand, 8);
        
        this.rawTerrain = new double[825];
        
        this.parabolicField = new float[25];

        for (int var5 = -2; var5 <= 2; ++var5)
        {
            for (int var6 = -2; var6 <= 2; ++var6)
            {
                float var7 = 10.0F / MathHelper.sqrt_float((float)(var5 * var5 + var6 * var6) + 0.2F);
                this.parabolicField[var5 + 2 + (var6 + 2) * 5] = var7;
            }
        }
	}
	
	@Override
	public boolean calculate( Cube cube )
	{		
		// get more biome data
		// NOTE: this is different from the column biome data for some reason...
		// Nick: This is a 10x10 array of biomes, centered on the xz center. points in the array are separated by 4 blocks.
		m_biomes = (CubeBiomeGenBase[])m_worldServer.getCubeWorldProvider().getWorldColumnMananger().getBiomesForGeneration(
			m_biomes,
			cube.getX()*4 - 2, cube.getZ()*4 - 2,
			9, 9
		);
		
		generateTerrain( cube );
		
		return true;
	}
	
	private double lerp(double a, double min, double max)
	{
		return min + a * (max - min);
	}
	
	public void generateTerrain(Cube cube)
    {
		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();
		
        byte seaLevel = 63;
        
        this.biomesForGeneration = (CubeBiomeGenBase[]) this.m_worldServer.getWorldChunkManager().getBiomesForGeneration(this.biomesForGeneration, cubeX * 4 - 2, cubeZ * 4 - 2, 10, 10);
        this.generateTerrainNoise(cubeX * 4, cubeY * 4, cubeZ * 4);

        for (int noiseX = 0; noiseX < 4; ++noiseX)
        {
            int x0 = noiseX * 5;
            int x1 = (noiseX + 1) * 5;

            for (int noiseZ = 0; noiseZ < 4; ++noiseZ)
            {
                int x0z0 = (x0 + noiseZ) * 33;
                int x0z1 = (x0 + noiseZ + 1) * 33;
                int x1z0 = (x1 + noiseZ) * 33;
                int x1z1 = (x1 + noiseZ + 1) * 33;

                for (int noiseY = 0; noiseY < 8; ++noiseY)
                {
                    double scale = 0.125D;
                    double x0z0y0 = this.rawTerrain[x0z0 + noiseY];
                    double x0z1y0 = this.rawTerrain[x0z1 + noiseY];
                    double x1z0y0 = this.rawTerrain[x1z0 + noiseY];
                    double x1z1y0 = this.rawTerrain[x1z1 + noiseY];
                    double x0z0y1 = (this.rawTerrain[x0z0 + noiseY + 1] - x0z0y0) * scale;
                    double x0z1y1 = (this.rawTerrain[x0z1 + noiseY + 1] - x0z1y0) * scale;
                    double x1z0y1 = (this.rawTerrain[x1z0 + noiseY + 1] - x1z0y0) * scale;
                    double x1z1y1 = (this.rawTerrain[x1z1 + noiseY + 1] - x1z1y0) * scale;

                    for (int stepY = 0; stepY < 8; ++stepY)
                    {
                    	int yRel = noiseY * 8 + stepY;
                    	
                        double var33 = 0.25D;
                        double var35 = x0z0y0;
                        double var37 = x0z1y0;
                        double var39 = (x1z0y0 - x0z0y0) * var33;
                        double var41 = (x1z1y0 - x0z1y0) * var33;

                        for (int stepX = 0; stepX < 4; ++stepX)
                        {
                        	int xRel = noiseX * 4 + stepX;
                        	
                            double var46 = 0.25D;
                            double var50 = (var37 - var35) * var46;
                            double var48 = var35 - var50;

                            for (int stepZ = 0; stepZ < 4; ++stepZ)
                            {
                            	int zRel = noiseZ * 4 + stepZ;
                            	
                                if ((var48 += var50) > 0.0D)
                                {
                                    cube.setBlockForGeneration(xRel, yRel, zRel, Blocks.stone);
                                }
                                else if (noiseY * 8 + stepY < seaLevel)
                                {
                                    cube.setBlockForGeneration(xRel,  yRel, zRel, Blocks.water);
                                }
                                else
                                {
                                    cube.setBlockForGeneration(xRel, yRel, zRel, null);
                                }
                            }

                            var35 += var39;
                            var37 += var41;
                        }

                        x0z0y0 += x0z0y1;
                        x0z1y0 += x0z1y1;
                        x1z0y0 += x1z0y1;
                        x1z1y0 += x1z1y1;
                    }
                }
            }
        }
    }

    public void func_147422_a(int chunkX, int chunkZ, Block[] blocks, byte[] meta, CubeBiomeGenBase[] biomes)
    {
        double var6 = 0.03125D;
        this.stoneNoise = this.noiseGen4.arrayNoise2D_pre(this.stoneNoise, (double)(chunkX * 16), (double)(chunkZ * 16), 16, 16, var6 * 2.0D, var6 * 2.0D, 1.0D);

        for (int xRel = 0; xRel < 16; ++xRel)
        {
            for (int zRel = 0; zRel < 16; ++zRel)
            {
                CubeBiomeGenBase biomeGen = biomes[zRel + xRel * 16];
                biomeGen.replaceBlocks_pre(this.m_worldServer, this.rand, blocks, meta, chunkX * 16 + xRel, chunkZ * 16 + zRel, this.stoneNoise[zRel + xRel * 16]);
            }
        }
    }

    private void generateTerrainNoise(int xOffset, int yOffset, int zOffset)
    {
        double xzScale = 684.412D;
        double yScale = 684.412D;
        double var8 = 512.0D;
        double var10 = 512.0D;
        
        this.noise6 = this.noiseGen6.generateNoiseOctaves(this.noise6, 
        		xOffset, zOffset, 
        		5, 5, 
        		200.0D, 200.0D, 
        		0.5D);
        this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, 
        		xOffset, yOffset, zOffset, 
        		5, 3, 5, 
        		xzScale / 80, yScale / 160, xzScale / 80);
        this.noise1 = this.noiseGen1.generateNoiseOctaves(this.noise1, 
        		xOffset, yOffset, zOffset, 
        		5, 3, 5, 
        		xzScale, yScale, xzScale);
        this.noise2 = this.noiseGen2.generateNoiseOctaves(this.noise2, 
        		xOffset, yOffset, zOffset, 
        		5, 3, 5, 
        		xzScale, yScale, xzScale);
        
        int loc3D = 0;
        int loc2D = 0;
        
        double var14 = 8.5D;

        for (int xStep = 0; xStep < 5; ++xStep)
        {
            for (int zStep = 0; zStep < 5; ++zStep)
            {
                float weightedSumVolatility = 0.0F;
                float weightedSumHeight = 0.0F;
                float weightedSum = 0.0F;
                byte smoothRange = 2;
                
                CubeBiomeGenBase biome = this.biomesForGeneration[xStep + 2 + (zStep + 2) * 10];

                for (int i = -smoothRange; i <= smoothRange; ++i)
                {
                    for (int j = -smoothRange; j <= smoothRange; ++j)
                    {
                        CubeBiomeGenBase biomeToWeigh = this.biomesForGeneration[xStep + i + 2 + (zStep + j + 2) * 10];
                        
                        float height = biomeToWeigh.biomeHeight;
                        float volatility = biomeToWeigh.biomeVolatility;

                        if (this.terrainType == WorldType.field_151360_e && height > 0.0F)
                        {
                            height = 1.0F + height * 2.0F;
                            volatility = 1.0F + volatility * 4.0F;
                        }

                        float biomeWeight = this.parabolicField[i + 2 + (j + 2) * 5] / (height + 2.0F);

                        if (biomeToWeigh.biomeHeight > biome.biomeHeight)
                        {
                            biomeWeight /= 2.0F;
                        }

                        weightedSumVolatility += volatility * biomeWeight;
                        weightedSumHeight += height * biomeWeight;
                        weightedSum += biomeWeight;
                    }
                }

                weightedSumVolatility /= weightedSum;
                weightedSumHeight /= weightedSum;
                
                weightedSumVolatility = weightedSumVolatility * 0.9F + 0.1F;
                weightedSumHeight = (weightedSumHeight * 4.0F - 1.0F) / 8.0F;
                
                // get the height noise in this section. This produces the actual terrain.
                double heightNoise = this.noise6[loc2D] / 8000.0D;

                if (heightNoise < 0.0D)
                {
                    heightNoise = -heightNoise * 0.3D; //invert and scale to 30%
                }

                heightNoise = heightNoise * 3.0D - 2.0D; // fudge it

                if (heightNoise < 0.0D)
                {
                    heightNoise /= 2.0D;

                    if (heightNoise < -1.0D) // clamp to -1.0
                    {
                        heightNoise = -1.0D;
                    }

                    heightNoise /= 1.4D;
                    heightNoise /= 2.0D;
                }
                else
                {
                    if (heightNoise > 1.0D) //clamp to 1.0
                    {
                        heightNoise = 1.0D;
                    }

                    heightNoise /= 8.0D;
                }

                ++loc2D;
                
                double var47 = (double)weightedSumHeight;
                double volatilityFactor = (double)weightedSumVolatility;
                
                var47 += heightNoise * 0.2D;
                var47 = var47 * 8.5D / 8.0D;
                
                double heightFactor = 8.5D + var47 * 4.0D;
                
                double maxElevation = 256.0D;

                for (int yStep = 0; yStep < 3; ++yStep)
                {
                    double offset = ((double)yStep - heightFactor) * 12.0D * 128.0D / maxElevation / volatilityFactor;

                    if (offset < 0.0D)
                    {
                        offset *= 4.0D;
                    }

                    double minHeight = this.noise1[loc3D] / 512.0D;
                    double maxHeight = this.noise2[loc3D] / 512.0D;
                    
                    double heightAlpha = (this.noise3[loc3D] / 10.0D + 1.0D) / 2.0D;
                    
                    double var40 = MathHelper.denormalizeClamp(minHeight, maxHeight, heightAlpha) - offset;

                    if (yStep > 29)
                    {
                        double var42 = (double)((float)(yStep - 29) / 3.0F);
                        var40 = var40 * (1.0D - var42) + -10.0D * var42;
                    }

                    this.rawTerrain[loc3D] = var40;
                    ++loc3D;
                }
            }
        }
    }

    /**
     * Checks to see if a chunk exists at x, y
     */
    public boolean chunkExists(int par1, int par2)
    {
        return true;
    }
}
