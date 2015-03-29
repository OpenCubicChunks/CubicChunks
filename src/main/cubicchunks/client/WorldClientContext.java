/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) 2014 Tall Worlds
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
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
