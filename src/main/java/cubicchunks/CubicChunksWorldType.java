/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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

import cubicchunks.api.generators.ITerrainGenerator;
import cubicchunks.generator.*;
import cubicchunks.generator.terrain.VanillaTerrainGenerator;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.ServerCubeCache;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;

public class CubicChunksWorldType extends WorldType implements ICubicChunksWorldType {

	public CubicChunksWorldType() {
		super("CustomCubic");
	}

	@Override public void registerWorldGen(WorldServer world, GeneratorPipeline pipeline) {

		ITerrainGenerator terrainGenerator = new VanillaTerrainGenerator(world.getSeed());

		ServerCubeCache cubeCache = (ServerCubeCache) world.getChunkProvider();
		// init the generator pipeline
		pipeline.addStage(GeneratorStage.TERRAIN, new TerrainProcessor(cubeCache, 5, terrainGenerator));
		pipeline.addStage(GeneratorStage.SURFACE, new SurfaceProcessor(cubeCache, 10, world.getSeed()));
		pipeline.addStage(GeneratorStage.STRUCTURES, new StructureProcessor("Features", cubeCache, 10));
		pipeline.addStage(GeneratorStage.LIGHTING, new FirstLightProcessor("Lighting", cubeCache, 5));
		pipeline.addStage(GeneratorStage.FEATURES, new FeatureProcessor("Population", world, cubeCache, 100));
	}

	public static void create() {
		new CubicChunksWorldType();
	}
}
