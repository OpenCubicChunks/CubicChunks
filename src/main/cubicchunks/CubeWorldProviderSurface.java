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
package cubicchunks;

import cubicchunks.generator.BiomeProcessor;
import cubicchunks.generator.FeatureProcessor;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.generator.PopulationProcessor;
import cubicchunks.generator.terrain.NewTerrainProcessor;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.CubeWorldServer;
import net.minecraft.util.Vec3;

public class CubeWorldProviderSurface extends CubeWorldProvider {
	
	@Override
	public String getDimensionName() {
		return "Cube-world surface";
	}
	
	@Override
	public int getAverageGroundLevel() {
		return getSeaLevel() + 1;
	}
	
	@Override
	public int getSeaLevel() {
		return 0;
	}
	
	@Override
	public float getCloudHeight() {
		return 256;
	}
	
	@Override
	public Vec3 getFogColor( float par1, float par2 ) {
		float r = 1 * 0.7529412F;
		float g = 1 * 0.84705883F;
		float b = 1.0F;
		return this.world.getWorldVec3Pool().getVecFromPool( (double)r, (double)g, (double)b );
	}
	
	@Override
	public boolean getWorldHasVoidParticles( ){
		return false;
	}
	
	@Override
	public double getVoidFogYFactor( )
	{
		return Double.MAX_VALUE;
	}
	
	@Override
	public GeneratorPipeline createGeneratorPipeline(CubeWorldServer worldServer) {
		GeneratorPipeline generatorPipeline = new GeneratorPipeline(worldServer.getCubeCache());
		generatorPipeline.addStage(GeneratorStage.Terrain, new NewTerrainProcessor("Terrain", worldServer, 10));
		generatorPipeline.addStage(GeneratorStage.Biomes, new BiomeProcessor("Biomes", worldServer, 10));
		generatorPipeline.addStage(GeneratorStage.Features, new FeatureProcessor("Features", worldServer.getCubeCache(), 10));
		generatorPipeline.addStage(GeneratorStage.Population, new PopulationProcessor("Population", worldServer, 10));
		generatorPipeline.addStage(GeneratorStage.Lighting, new FirstLightProcessor("Lighting", worldServer.getCubeCache(), 10));
		return generatorPipeline;
	}

	@Override
	public String getDimensionNameSuffix() {
		return "";
	}
}
