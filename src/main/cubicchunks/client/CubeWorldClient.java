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
package cubicchunks.client;

import net.minecraft.network.play.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.IChunkGenerator;
import cubicchunks.CubeCache;
import cubicchunks.CubeProviderTools;
import cubicchunks.CubeWorld;
import cubicchunks.accessors.WorldClientAccessor;
import cubicchunks.lighting.LightingManager;

public class CubeWorldClient extends WorldClient implements CubeWorld {
	
	private LightingManager m_lightingManager;
	
	public CubeWorldClient(NetHandlerPlayClient client, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profiler) {
		super(client, settings, dimension, difficulty, profiler);
	}
	
	@Override
	protected IChunkGenerator createChunkCache() {
		CubeProviderClient chunkProvider = new CubeProviderClient(this);
		WorldClientAccessor.setChunkProvider(this, chunkProvider);
		
		// init the lighting manager
		this.m_lightingManager = new LightingManager(this, chunkProvider);
		
		return (IChunkGenerator) chunkProvider;
	}
	
	@Override
	public CubeCache getCubeCache() {
		return (CubeCache)this.chunkCache;
	}
	
	@Override
	public LightingManager getLightingManager() {
		return this.m_lightingManager;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		this.m_lightingManager.tick();
	}
	
	@Override
	public boolean updateLightingAt(LightType lightType, BlockPos pos) {
		// forward to the new lighting system
		return this.m_lightingManager.computeDiffuseLighting(pos, lightType);
	}
	
	@Override
	public boolean checkBlockRangeIsInWorld(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, boolean flag) {
		return CubeProviderTools.blocksExist((CubeCache)this.clientChunkCache, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
	}
}
