package cubicchunks.server;

import java.util.Map;

import net.minecraft.world.WorldServer;

import com.google.common.collect.Maps;

import cubicchunks.generator.BiomeProcessor;
import cubicchunks.generator.FeatureProcessor;
import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.generator.PopulationProcessor;
import cubicchunks.generator.terrain.NewTerrainProcessor;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.lighting.LightingManager;
import cubicchunks.world.WorldContext;


public class WorldServerContext implements WorldContext {
	
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
	private LightingManager m_lightingManager;
	private GeneratorPipeline m_generatorPipeline;
	
	public WorldServerContext(WorldServer worldServer, ServerCubeCache serverCubeCache) {
		
		m_worldServer = worldServer;
		m_serverCubeCache = serverCubeCache;
		m_lightingManager = new LightingManager(worldServer, serverCubeCache);
		m_generatorPipeline = new GeneratorPipeline(serverCubeCache);
		
		// init the generator pipeline
		m_generatorPipeline.addStage(GeneratorStage.Terrain, new NewTerrainProcessor("Terrain", m_worldServer, m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.Biomes, new BiomeProcessor("Biomes", m_worldServer, m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.Features, new FeatureProcessor("Features", m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.Population, new PopulationProcessor("Population", m_serverCubeCache, 10));
		m_generatorPipeline.addStage(GeneratorStage.Lighting, new FirstLightProcessor("Lighting", m_serverCubeCache, 10));
	}
	
	public WorldServer getWorldServer() {
		return m_worldServer;
	}
	
	@Override
	public ServerCubeCache getCubeCache() {
		return m_serverCubeCache;
	}
	
	@Override
	public LightingManager getLightingManager() {
		return m_lightingManager;
	}
	
	public GeneratorPipeline getGeneratorPipeline() {
		return m_generatorPipeline;
	}
}
