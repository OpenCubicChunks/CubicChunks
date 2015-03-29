package cubicchunks.client;

import java.util.Map;

import net.minecraft.world.WorldClient;

import com.google.common.collect.Maps;

import cubicchunks.lighting.LightingManager;
import cubicchunks.world.WorldContext;


public class WorldClientContext implements WorldContext {
	
	private static Map<WorldClient,WorldClientContext> m_instances;
	
	static {
		m_instances = Maps.newHashMap();
	}
	
	public static WorldClientContext get(WorldClient worldClient) {
		return m_instances.get(worldClient);
	}
	
	public static void put(WorldClient worldClient, WorldClientContext worldClientContext) {
		m_instances.put(worldClient, worldClientContext);
	}
	
	private WorldClient m_worldClient;
	private ClientCubeCache m_clientCubeCache;
	private LightingManager m_lightingManager;
	
	public WorldClientContext(WorldClient worldClient, ClientCubeCache clientCubeCache) {
		m_worldClient = worldClient;
		m_clientCubeCache = clientCubeCache;
		m_lightingManager = new LightingManager(worldClient, clientCubeCache);
	}
	
	public WorldClient getWorldClient() {
		return m_worldClient;
	}
	
	@Override
	public ClientCubeCache getCubeCache() {
		return m_clientCubeCache;
	}
	
	@Override
	public LightingManager getLightingManager() {
		return m_lightingManager;
	}
}
