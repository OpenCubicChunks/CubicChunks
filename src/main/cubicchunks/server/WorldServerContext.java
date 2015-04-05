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
package cubicchunks.server;

import java.util.Map;

import net.minecraft.world.WorldServer;

import com.google.common.collect.Maps;

import cubicchunks.generator.BiomeProcessor;
import cubicchunks.generator.StructureProcessor;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.generator.FeatureProcessor;
import cubicchunks.generator.terrain.NewTerrainProcessor;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.world.WorldContext;


public class WorldServerContext extends WorldContext {
	
	private static Map<WorldServer,WorldServerContext> m_instances;
	
	static {
		m_instances = Maps.newHashMap();
	}
	
	public static WorldServerContext get(WorldServer worldServer) {
		return m_instances.get(worldServer);
	}
	
	public static void put(WorldServer worldServer, WorldServerContext worldServerContext) {
		m_instances.put(worldServer, worldServerContext);
	}
	
	private WorldServer m_worldServer;
	private ServerCubeCache m_serverCubeCache;
	private GeneratorPipeline m_generatorPipeline;
	
	public WorldServerContext(WorldServer worldServer, ServerCubeCache serverCubeCache) {
		super(worldServer, serverCubeCache);
		
		m_worldServer = worldServer;
		m_serverCubeCache = serverCubeCache;
		m_generatorPipeline = new GeneratorPipeline(serverCubeCache);
		
		// init the generator pipeline
		m_generatorPipeline.addStage(GeneratorStage.TERRAIN, new NewTerrainProcessor("Terrain", m_worldServer, m_serverCubeCache, 5));
		m_generatorPipeline.addStage(GeneratorStage.BIOMES, new BiomeProcessor("Biomes", m_worldServer, m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.STRUCTURES, new StructureProcessor("Features", m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.LIGHTING, new FirstLightProcessor("Lighting", m_serverCubeCache, 5));
		m_generatorPipeline.addStage(GeneratorStage.FEATURES, new FeatureProcessor("Population", m_serverCubeCache, 100));
		m_generatorPipeline.checkStages();
	}
	
	@Override
	public WorldServer getWorld() {
		return m_worldServer;
	}
	
	@Override
	public ServerCubeCache getCubeCache() {
		return m_serverCubeCache;
	}
	
	public GeneratorPipeline getGeneratorPipeline() {
		return m_generatorPipeline;
	}
}
