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
package cuchaz.cubicChunks;

import cuchaz.cubicChunks.generator.BiomeProcessor;
import cuchaz.cubicChunks.generator.FeatureProcessor;
import cuchaz.cubicChunks.generator.GeneratorPipeline;
import cuchaz.cubicChunks.generator.GeneratorStage;
import cuchaz.cubicChunks.generator.PopulationProcessor;
import cuchaz.cubicChunks.generator.TerrainProcessor;
import cuchaz.cubicChunks.lighting.FirstLightProcessor;
import cuchaz.cubicChunks.server.CubeWorldServer;


public class CubeWorldProviderSurface extends CubeWorldProvider
{
	@Override
	public String getDimensionName( )
	{
		return "Cube-world surface";
	}
	
	@Override
	public int getAverageGroundLevel( )
	{
		return getSeaLevel() + 1;
	}
	
	@Override
	public int getSeaLevel( )
	{
		return 0;
	}
	
	@Override
	public float getCloudHeight()
	{
		return 256;
	}

	@Override
	public GeneratorPipeline createGeneratorPipeline( CubeWorldServer worldServer )
	{
		GeneratorPipeline generatorPipeline = new GeneratorPipeline( worldServer.getCubeProvider() );
		generatorPipeline.addStage( GeneratorStage.Terrain, new TerrainProcessor( "Terrain", worldServer, 10 ) );
		generatorPipeline.addStage( GeneratorStage.Biomes, new BiomeProcessor( "Biomes", worldServer, 10 ) );
		generatorPipeline.addStage( GeneratorStage.Features, new FeatureProcessor( "Features", worldServer.getCubeProvider(), 10 ) );
		generatorPipeline.addStage( GeneratorStage.Population, new PopulationProcessor( "Population", worldServer, 10 ) );
		generatorPipeline.addStage( GeneratorStage.Lighting, new FirstLightProcessor( "Lighting", worldServer.getCubeProvider(), 10 ) );
		return generatorPipeline;
	}
}
