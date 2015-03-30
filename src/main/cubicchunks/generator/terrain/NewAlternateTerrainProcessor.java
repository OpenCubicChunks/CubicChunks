/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.generator.terrain;

import cubicchunks.generator.biome.alternateGen.AlternateWorldColumnManager;
import cubicchunks.generator.builder.BasicBuilder;
import cubicchunks.generator.builder.IBuilder;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.X_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Y_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.Z_SECTIONS;
import static cubicchunks.generator.terrain.GlobalGeneratorConfig.maxElev;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.Coords;
import cubicchunks.world.Cube;
import java.util.Random;
import net.minecraft.block.Blocks;

/**
 *
 * @author Barteks2x
 */
public class NewAlternateTerrainProcessor extends AbstractTerrainProcessor3dNoise {

	private int genToNormal[] = { 0, 4, 8, 10, 16 };

	private final AlternateWorldColumnManager wcm;

	private static final int octaves = 16;

	private final Random m_rand;

	private double[][] noiseArrayHeight;
	private double[][] noiseArrayVolatility;
	private double[][] noiseArrayTemp;
	private double[][] noiseArrayRainfall;

	public NewAlternateTerrainProcessor(String name, CubeWorldServer worldServer, int batchSize) {
		super(name, worldServer, batchSize);

		this.wcm = (AlternateWorldColumnManager) worldServer.getBiomeManager();

		this.m_rand = new Random(m_worldServer.getSeed());

		this.noiseArrayHeight = new double[X_SECTIONS][Z_SECTIONS];
		this.noiseArrayVolatility = new double[X_SECTIONS][Z_SECTIONS];
	}

	@Override
	protected IBuilder createHighBuilder() {
		Random rand = new Random(m_worldServer.getSeed());

		BasicBuilder builderHigh = new BasicBuilder();
		builderHigh.setSeed(rand.nextInt());
		builderHigh.setOctaves(octaves);
		builderHigh.setMaxElev(150);
		builderHigh.setSeaLevel(seaLevel / maxElev);
		// builderHigh.setFreq( freqHor, freqVert, freqHor );
		builderHigh.setFreq(0.011731323D);
		// builderHigh.setClamp( -5, 5 );
		builderHigh.setlacunarity(2);
		builderHigh.build();

		return builderHigh;
	}

	@Override
	protected IBuilder createLowBuilder() {
		Random rand = new Random(m_worldServer.getSeed() * 2);

		BasicBuilder builderLow = new BasicBuilder();
		builderLow.setSeed(rand.nextInt());
		builderLow.setOctaves(octaves);
		builderLow.setMaxElev(150);
		builderLow.setSeaLevel(seaLevel / maxElev);
		// builderLow.setFreq( freqHor, freqVert, freqHor );
		builderLow.setFreq(0.011731323D);
		// builderLow.setClamp( -5, 5 );
		builderLow.setlacunarity(2);
		builderLow.build();

		return builderLow;
	}

	@Override
	protected IBuilder createAlphaBuilder() {
		Random rand = new Random(m_worldServer.getSeed() * 3);

		BasicBuilder builderAlpha = new BasicBuilder();
		builderAlpha.setSeed(rand.nextInt());
		builderAlpha.setOctaves(octaves / 2);
		builderAlpha.setMaxElev(5);
		// builderAlpha.setFreq( freqHor, freqVert, freqHor );
		builderAlpha.setFreq(0.066837109D);
		builderAlpha.setSeaLevel(0.5D);
		builderAlpha.setClamp(0, 1);
		builderAlpha.setlacunarity(2);
		builderAlpha.build();

		return builderAlpha;
	}

	@Override
	public boolean calculate(Cube cube) {
		CubeWorldServer world = (CubeWorldServer) cube.getWorld();
		AlternateWorldColumnManager wcm = (AlternateWorldColumnManager) world.getBiomeManager();
		/*
		 * double [][] height = wcm.getHeightArray(cube.getX(), cube.getZ());
		 * //double [][] volatility = wcm.getVolArray(cube.getX(), cube.getZ());
		 * //double [][] temp = wcm.getTempArray(cube.getX(), cube.getZ());
		 * //double [][] hum = wcm.getRainfallArray(cube.getX(), cube.getZ());
		 * int heightMax = -1999; int heightMin = 1999; double volMax = 0; for
		 * (int i = 0; i < 16; i++){ for (int j = 0; j < 16; j++){ int height1 =
		 * (int)wcm.getRealHeight(height[i][j]); //double volatility1 =
		 * wcm.getRealVolatility(volatility[i][j], height[i][j], hum[i][j],
		 * temp[i][j]); heightMax = heightMax > height1 ? heightMax : height1 ;
		 * heightMin = heightMin < height1 ? heightMin : height1 ; //volMax =
		 * volMax > volatility1 ? volMax : volatility1 ; } } int buffer = 10;//
		 * +/-3 cubes if ( cube.getY() * 2 < heightMin - buffer){return
		 * setToStone(cube);} if ( cube.getY() * 2 > heightMax + buffer){return
		 * setToAir(cube);}
		 */
		return calculateSurface(cube);
	}

	public boolean setToStone(Cube cube) {
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					cube.setBlockForGeneration(i, j, k, Blocks.stone);
				}
			}
		}
		return true;
	}

	public boolean setToAir(Cube cube) {
		for (int j = 0; j < 16; j++) {
			if (Coords.localToBlock(cube.getY(), j) > seaLevel) {
				for (int i = 0; i < 16; i++) {
					for (int k = 0; k < 16; k++) {
						cube.setBlockForGeneration(i, j, k, Blocks.air);
					}
				}
			} else {
				for (int i = 0; i < 16; i++) {
					for (int k = 0; k < 16; k++) {
						cube.setBlockForGeneration(i, j, k, Blocks.water);
					}
				}
			}
		}
		return true;
	}

	public boolean calculateSurface(Cube cube) {

		return super.calculate(cube);
	}

	protected void generateTerrainArray(Cube cube) {
		this.noiseArrayHeight = wcm.getHeightArray(cube.getX(), cube.getZ());
		this.noiseArrayVolatility = wcm.getVolArray(cube.getX(), cube.getZ());

		this.noiseArrayRainfall = wcm.getRainfallArray(cube.getX(), cube.getZ());
		this.noiseArrayTemp = wcm.getTempArray(cube.getX(), cube.getZ());

		for (int x = 0; x < X_SECTIONS; x++) {
			for (int z = 0; z < Z_SECTIONS; z++) {
				double height = noiseArrayHeight[genToNormal[x]][genToNormal[z]];
				double vol = noiseArrayVolatility[genToNormal[x]][genToNormal[z]];
				double temp = noiseArrayTemp[genToNormal[x]][genToNormal[z]];
				double rainfall = noiseArrayRainfall[genToNormal[x]][genToNormal[z]];
				// height *= 1 - vol;
				vol = this.wcm.getRealVolatility(vol, height, rainfall, temp) * 1.4 + 0.04D;
				height = this.wcm.getRealHeight(height) * (50D + 250D * vol);
				double output;
				double d8;
				int yPos = cube.getY() * (Y_SECTIONS - 1);
				for (int y = 0; y < Y_SECTIONS; y++) {
					d8 = (yPos + y - height) / vol / 4 * 3;
					if (d8 < 0.0D) {
						d8 *= 4.0D;
					}

					final double vol1Low = this.noiseArrayLow[x][y][z] /*
																		 * biomeConfig
																		 * .
																		 * volatility1
																		 */;
					final double vol2High = this.noiseArrayHigh[x][y][z] /*
																		 * biomeConfig
																		 * .
																		 * volatility2
																		 */;
					final double noiseAlpha = this.noiseArrayAlpha[x][y][z];
					output = vol1Low + (vol2High - vol1Low) * noiseAlpha;
					output -= d8;
					this.rawTerrainArray[x][y][z] = output;
				}
			}
		}
	}
}
