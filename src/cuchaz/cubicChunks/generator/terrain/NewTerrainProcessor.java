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

import net.minecraft.util.MathHelper;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.world.Cube;
import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.*;

public class NewTerrainProcessor extends AbstractTerrainProcessor3dNoise
{
	private CubeBiomeGenBase[] m_biomes;
	
	private Random m_rand;

    private double volatilityFactor;
    private double heightFactor;

    private final int maxSmoothRadius = 4;
    private final int maxSmoothDiameter = maxSmoothRadius*2 + 1;
	
	private double[][] noiseArrayHeight;
	
	private double[] nearBiomeWeightArray;
	
	private static BasicBuilder builderHeight;
	
	private static int octaves = 16; 
	
	public NewTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer, batchSize );
		
		this.m_biomes = null;
		
		this.m_rand = new Random( m_worldServer.getSeed() );
		
		this.noiseArrayHeight = new double[X_NOISE_SIZE][Z_NOISE_SIZE];
		
        this.nearBiomeWeightArray = new double[maxSmoothDiameter * maxSmoothDiameter];

        for (int x = -maxSmoothRadius; x <= maxSmoothRadius; x++)
        {
            for (int z = -maxSmoothRadius; z <= maxSmoothRadius; z++)
            {
                final double f1 = 10.0F / Math.sqrt(x * x + z * z + 0.2F);
                this.nearBiomeWeightArray[(x + maxSmoothRadius + (z + maxSmoothRadius) * maxSmoothDiameter)] = f1;
            }
        }
        
        double freq = 1 / (4*Math.PI);

		builderHeight = new BasicBuilder();
		builderHeight.setSeed( m_rand.nextInt() );
		builderHeight.setOctaves( octaves );
		builderHeight.setMaxElev( 1 );
		builderHeight.setFreq( freq / 3 );
		builderHeight.build();
	}
	
	@Override
	protected IBuilder createHighBuilder() {
		Random rand = new Random( m_worldServer.getSeed() * 2 );
		double freq = 1 / (4*Math.PI);
		
		BasicBuilder builderHigh = new BasicBuilder();
		builderHigh.setSeed( rand.nextInt() );
		builderHigh.setOctaves( octaves );
		builderHigh.setMaxElev( 1 );
		builderHigh.setFreq( freq, freq * 4, freq );
		builderHigh.build();
		
		return builderHigh;
	}

	@Override
	protected IBuilder createLowBuilder() {
		Random rand = new Random( m_worldServer.getSeed() * 3 );
		double freq = 1 / (4*Math.PI);
		
		BasicBuilder builderLow = new BasicBuilder();
		builderLow.setSeed( rand.nextInt() );
		builderLow.setOctaves( octaves );
		builderLow.setMaxElev( 1 );
		builderLow.setFreq( freq, freq * 4, freq );
		builderLow.build();
		
		return builderLow;
	}

	@Override
	protected IBuilder createAlphaBuilder() {
		Random rand = new Random( m_worldServer.getSeed() * 4 );
		double freq = 1 / (4*Math.PI);
		
		BasicBuilder builderAlpha = new BasicBuilder();
		builderAlpha.setSeed( rand.nextInt() );
		builderAlpha.setOctaves( octaves );
		builderAlpha.setMaxElev( 10 );
		builderAlpha.setFreq( freq, freq * 4, freq );
		builderAlpha.build();
		
		return builderAlpha;
	}

	protected void generateTerrainArray(Cube cube)
	{
		m_biomes = (CubeBiomeGenBase[])m_worldServer.getCubeWorldProvider().getWorldColumnMananger().getBiomesForGeneration(
			m_biomes,
			cube.getX() * 4 - maxSmoothRadius, cube.getZ() * 4 - maxSmoothRadius,
			X_NOISE_SIZE + maxSmoothDiameter, Z_NOISE_SIZE + maxSmoothDiameter
		);
		
		for (int x = 0; x < X_NOISE_SIZE; x++)
        {
            for (int z = 0; z < Z_NOISE_SIZE; z++)
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
                    noiseHeight /= 1.4D;
                    noiseHeight /= 2.0D;

                } 
                else
                {
                    if (noiseHeight > 1.0D)
                    {
                        noiseHeight = 1.0D;
                    }
                    noiseHeight /= 8.0D;
                }
                this.biomeFactor(x, z, noiseHeight);

                for (int y = 0; y < Y_NOISE_SIZE; y++)
                {
                    double output;
					double heightModifier;
					double volatilityModifier;

					heightModifier = this.heightFactor;
					volatilityModifier = this.volatilityFactor;

					double vol1Low = MathHelper.clamp_double( this.noiseArrayLow[x][y][z], -1, 1 );
					double vol2High = MathHelper.clamp_double( this.noiseArrayHigh[x][y][z], -1, 1 );

					final double noiseAlpha = this.noiseArrayAlpha[x][y][z];

					if( noiseAlpha < 0 )
					{
						output = vol1Low;
					}
					else if( noiseAlpha > 1 )
					{
						output = vol2High;
					}
					else
					{
						output = vol1Low + (vol2High - vol1Low) * noiseAlpha;
					}

					//make height range lower
					output *= volatilityModifier;
					//height shift
					output += heightModifier;

					int maxYSections = MathHelper.ceiling_double_int( maxElev / 2 );
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
	
    private void biomeFactor(int x, int z, double noiseHeight)
    {
        float volatilitySum = 0.0F;
        double heightSum = 0.0F;
        float biomeWeightSum = 0.0F;

        final CubeBiomeGenBase centerBiomeConfig = this.m_biomes[(x + this.maxSmoothRadius + (z + this.maxSmoothRadius)
                * (X_NOISE_SIZE + this.maxSmoothDiameter))];
        final int lookRadius = 2;

        float nextBiomeHeight;
		double biomeWeight;

        for (int nextX = -lookRadius; nextX <= lookRadius; nextX++)
        {
            for (int nextZ = -lookRadius; nextZ <= lookRadius; nextZ++)
            {
                final CubeBiomeGenBase nextBiomeConfig = this.m_biomes[(x + nextX + this.maxSmoothRadius + (z + nextZ + this.maxSmoothRadius)
                        * (X_NOISE_SIZE + this.maxSmoothDiameter))];

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

        this.volatilityFactor = volatilitySum;
        this.heightFactor = heightSum < 0 ? heightSum / 4 : heightSum;//4 * (heightSum + noiseHeight * 0.2D);
		this.heightFactor += noiseHeight * 0.2;
		
		heightFactor *= 1 - volatilityFactor;
    }
}
