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

import cubicchunks.util.WorldAccess;
import cubicchunks.util.WorldClientAccess;
import cubicchunks.util.WorldServerAccess;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CCEventHandler {
	private final CubicChunkSystem cc;
	
	public CCEventHandler(CubicChunkSystem cc) {
		this.cc = cc;
	}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load evt) {
		CubicChunks.LOGGER.info("Initializing world " + evt.world + " with type " + evt.world.getWorldType());
		World world = evt.world;
		if(world instanceof WorldServer && cc.isTallWorld(world)) {
			modifyWorld((WorldServer)world);
			cc.generateWorld((WorldServer)world);
		}
		if(world instanceof WorldClient && cc.isTallWorld(world)) {
			modifyWorld((WorldClient)world);
		}
	}

	private void modifyWorld(WorldServer worldServer) {
		ChunkProviderServer cubeCache = cc.getServerChunkCacheAndInitWorld(worldServer);
		worldServer.theChunkProviderServer = cubeCache;
		WorldAccess.setChunkProvider(worldServer, cubeCache);
		WorldServerAccess.setPlayerManager(worldServer, cc.getPlayerManager(worldServer));
		worldServer.getGameRules().setOrCreateGameRule("doMobSpawning", String.valueOf(false));
	}
	
	private void modifyWorld(WorldClient worldClient) {
		ChunkProviderClient cubeCache = cc.getClientChunkCacheAndInitWorld(worldClient);
		WorldClientAccess.setChunkProviderClient(worldClient, cubeCache);
		WorldAccess.setChunkProvider(worldClient, cubeCache);
	}
}
