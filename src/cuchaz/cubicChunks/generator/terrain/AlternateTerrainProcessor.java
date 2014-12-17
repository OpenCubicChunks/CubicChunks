/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuchaz.cubicChunks.generator.terrain;

import cuchaz.cubicChunks.generator.biome.alternateGen.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.world.Cube;

import java.util.Random;

import net.minecraft.util.MathHelper;
import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.*;

/**
 *
 * @author Barteks2x
 */
public class AlternateTerrainProcessor extends AbstractTerrainProcessor3dNoise
{
	private static final int genToNormal[] =
	{
		0, 4, 8, 12, 16
	};

	private final AlternateWorldColumnManager wcm;

	private final int octaves = 16;

	private double[][] noiseArrayHeight;
	private double[][] noiseArrayVolatility;
	private double[][] noiseArrayTemp;
	private double[][] noiseArrayRainfall;

	public AlternateTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer, batchSize );

		this.wcm = (AlternateWorldColumnManager)worldServer.getWorldChunkManager();
		this.amplify = true;
	}
	
	@Override
	protected IBuilder createHighBuilder() {
		Random rand = new Random( m_worldServer.getSeed() );
		
		double freqHor = Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		
		BasicBuilder builderHigh = new BasicBuilder();
		builderHigh.setSeed( rand.nextInt() );
		builderHigh.setOctaves( octaves );
		builderHigh.setMaxElev( 1 );
		builderHigh.setSeaLevel( seaLevel / maxElev );
		builderHigh.setFreq( freqHor, freqVert, freqHor );
		builderHigh.build();
		
		return builderHigh;
	}

	@Override
	protected IBuilder createLowBuilder() {
		Random rand = new Random( m_worldServer.getSeed() * 2 );
		
		double freqHor = Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		
		BasicBuilder builderLow = new BasicBuilder();
		builderLow.setSeed( rand.nextInt() );
		builderLow.setOctaves( octaves );
		builderLow.setMaxElev( 1 );
		builderLow.setSeaLevel( seaLevel / maxElev );
		builderLow.setFreq( freqHor, freqVert, freqHor );
		builderLow.build();
		
		return builderLow;
	}

	@Override
	protected IBuilder createAlphaBuilder() {
		Random rand = new Random( m_worldServer.getSeed() * 3);
		
		double freqHor = Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		double freqVert = 4 * Math.min( 1, 128 / maxElev ) / (4 * Math.PI);
		
		BasicBuilder builderAlpha = new BasicBuilder();
		builderAlpha.setSeed( rand.nextInt() );
		builderAlpha.setOctaves( octaves / 2 );
		builderAlpha.setMaxElev( 10 );
		builderAlpha.setFreq( freqHor, freqVert, freqHor );
		builderAlpha.build();
		
		return builderAlpha;
	}

	protected void generateTerrainArray( Cube cube )
	{
		this.noiseArrayHeight = wcm.getHeightArray( cube.getX(), cube.getZ() );
		this.noiseArrayVolatility = wcm.getVolArray( cube.getX(), cube.getZ() );

		this.noiseArrayRainfall = wcm.getRainfallArray( cube.getX(), cube.getZ() );
		this.noiseArrayTemp = wcm.getTempArray( cube.getX(), cube.getZ() );
		
		for( int x = 0; x < X_SECTION_SIZE; x++ )
		{
			for( int z = 0; z < Z_SECTION_SIZE; z++ )
			{
				double height = noiseArrayHeight[genToNormal[x]][genToNormal[z]];
				double vol = noiseArrayVolatility[genToNormal[x]][genToNormal[z]];
				double temp = noiseArrayTemp[genToNormal[x]][genToNormal[z]];
				double rainfall = noiseArrayRainfall[genToNormal[x]][genToNormal[z]];

				height = this.wcm.getRealHeight( height );
				vol = this.wcm.getRealVolatility( vol, height, rainfall, temp );
				rainfall *= temp;

				for( int y = 0; y < Y_SECTION_SIZE; y++ )
				{
					double vol1Low = MathHelper.clamp_double( this.noiseArrayLow[x][y][z], -1, 1 );
					double vol2High = MathHelper.clamp_double( this.noiseArrayHigh[x][y][z], -1, 1 );
					double noiseAlpha = this.noiseArrayAlpha[x][y][z] * 10;

					double output =  lerp( MathHelper.clamp_double(noiseAlpha, 0, 1), vol1Low, vol2High );

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
}
