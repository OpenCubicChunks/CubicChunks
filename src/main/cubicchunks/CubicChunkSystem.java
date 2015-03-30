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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.gen.ClientChunkCache;
import net.minecraft.world.gen.ServerChunkCache;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.server.CubePlayerManager;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.server.WorldServerContext;
import cubicchunks.util.AddressTools;
import cuchaz.m3l.api.chunks.ChunkSystem;

public class CubicChunkSystem implements ChunkSystem {
	
	@Override
	public ServerChunkCache getServerChunkCache(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			ServerCubeCache serverCubeCache = new ServerCubeCache(worldServer);
			WorldServerContext.put(worldServer, new WorldServerContext(worldServer, serverCubeCache));
			return serverCubeCache;
		}
		return null;
	}
	
	@Override
	public ClientChunkCache getClientChunkCache(WorldClient worldClient) {
		if (isTallWorld(worldClient)) {
			ClientCubeCache clientCubeCache = new ClientCubeCache(worldClient);
			WorldClientContext.put(worldClient, new WorldClientContext(worldClient, clientCubeCache));
			return clientCubeCache;
		}
		return null;
	}
	
	@Override
	public BiomeManager getBiomeManager(World world) {
		
		/* TEMP: disable fancy generation
		// for now, only muck with the overworld
		if (world.dimension.getId() == 0) {
			DimensionType dimensionType = world.getWorldInfo().getDimensionType();
			if (dimensionType != DimensionType.FLAT && dimensionType != DimensionType.DEBUG_ALL_BLOCK_STATES) {
				return new CCBiomeManager(world);
			}
		}
		*/
		
		return null;
	}

	@Override
	public int getMinBlockY() {
		return AddressTools.MinY;
	}

	@Override
	public int getMaxBlockY() {
		return AddressTools.MaxY;
	}

	@Override
	public PlayerManager getPlayerManager(WorldServer worldServer) {
		if (isTallWorld(worldServer)) {
			return new CubePlayerManager(worldServer);
		}
		return null;
	}

	@Override
	public void processChunkLoadQueue(EntityPlayerMP player) {
		WorldServer worldServer = (WorldServer)player.getWorld();
		if (isTallWorld(worldServer)) {
			CubePlayerManager playerManager = (CubePlayerManager)worldServer.getPlayerManager();
			playerManager.processCubeLoadQueue(player);
		}
	}
	
	private boolean isTallWorld(World world) {
		// for now, only tall-ify the overworld
		return world.dimension.getId() == 0;
	}
}
