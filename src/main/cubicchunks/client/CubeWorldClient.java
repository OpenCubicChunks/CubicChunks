/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
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

import cubicchunks.CubeCache;
import cubicchunks.CubeProviderTools;
import cubicchunks.CubeWorld;
import cubicchunks.accessors.WorldClientAccessor;
import cubicchunks.lighting.LightingManager;
import net.minecraft.world.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

public class CubeWorldClient extends WorldClient implements CubeWorld {
	
	private LightingManager m_lightingManager;
	
	public CubeWorldClient(NetHandlerPlayClient client, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profiler) {
		super(client, settings, dimension, difficulty, profiler);
	}
	
	@Override
	protected IChunkProvider createChunkProvider() {
		CubeProviderClient chunkProvider = new CubeProviderClient(this);
		WorldClientAccessor.setChunkProvider(this, chunkProvider);
		
		// init the lighting manager
		m_lightingManager = new LightingManager(this, chunkProvider);
		
		return chunkProvider;
	}
	
	@Override
	public CubeCache getCubeProvider() {
		return (CubeCache)chunkProvider;
	}
	
	@Override
	public LightingManager getLightingManager() {
		return m_lightingManager;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		m_lightingManager.tick();
	}
	
	@Override
	public boolean updateLightByType(EnumSkyBlock lightType, int blockX, int blockY, int blockZ) {
		// forward to the new lighting system
		return m_lightingManager.computeDiffuseLighting(blockX, blockY, blockZ, lightType);
	}
	
	@Override
	public boolean checkChunksExist(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
		return CubeProviderTools.blocksExist((CubeCache)chunkProvider, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
	}
}
