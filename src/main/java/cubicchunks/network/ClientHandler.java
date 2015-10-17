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
package cubicchunks.network;

import cubicchunks.CubicChunks;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.AddressTools;
import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.INetHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldProviderSurface;

public class ClientHandler implements INetHandler {
	
	private static ClientHandler m_instance;
	
	public static ClientHandler getInstance() {
		if (m_instance == null) {
			m_instance = new ClientHandler();
		}
		return m_instance;
	}
	
	@Override
	public void onDisconnect(IChatComponent chat) {
		// nothing to do
	}
	
	public void handle(final PacketBulkCubeData packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			return;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().theWorld;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		packet.startDecoding();
		
		// load columns first
		for (long columnAddress : packet.columnAddresses) {
			Column column = cubeCache.loadChunk(AddressTools.getX(columnAddress), AddressTools.getZ(columnAddress));
			packet.decodeNextColumn(column);
		}
		
		// then cubes
		int outOfOrderCubes = 0;
		for (long cubeAddress : packet.cubeAddresses) {
			int cubeX = AddressTools.getX(cubeAddress);
			int cubeY = AddressTools.getY(cubeAddress);
			int cubeZ = AddressTools.getZ(cubeAddress);
			Column column = cubeCache.getColumn(cubeX, cubeZ);
			if (column instanceof BlankColumn) {
				outOfOrderCubes++;
				
				// still need to read the cube though
				packet.decodeNextCube(new Cube(worldClient, column, 0, 0, 0, false));
				
				continue;
			}
			Cube cube = column.getOrCreateCube(cubeY, false);
			packet.decodeNextCube(cube);
			cube.markForRenderUpdate();
		}
		if (outOfOrderCubes > 0) {
			CubicChunks.LOGGER.error("Ignored {}/{} cubes that arrived before their columns", outOfOrderCubes, packet.cubeAddresses.length);
		}
		
		packet.finishDecoding();
		
		// update column metadata
		for (long columnAddress : packet.columnAddresses) {
			Column column = cubeCache.getColumn(AddressTools.getX(columnAddress), AddressTools.getZ(columnAddress));
			
			// update lighting flags
			if (! (worldClient.provider instanceof WorldProviderSurface)) {
				column.resetRelightChecks();
			}
			column.setTerrainPopulated(true);
			
			// update tile entities in each chunk
			for (Cube cube : column.getCubes()) {
				for (TileEntity blockEntity : cube.getBlockEntities()) {
					blockEntity.updateContainingBlockInfo();
				}
			}
		}
	}
	
	public void handle(final PacketUnloadCubes packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			return;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().theWorld;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		for (long cubeAddress : packet.cubeAddresses) {
			cubeCache.unloadCube(
				AddressTools.getX(cubeAddress),
				AddressTools.getY(cubeAddress),
				AddressTools.getZ(cubeAddress)
			);
		}
	}
	
	public void handle(final PacketUnloadColumns packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			return;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().theWorld;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		for (long cubeAddress : packet.columnAddresses) {
			cubeCache.unloadColumn(
				AddressTools.getX(cubeAddress),
				AddressTools.getZ(cubeAddress)
			);
		}
	}
	
	public void handle(final PacketCubeChange packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			return;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().theWorld;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			CubicChunks.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// apply the update
		packet.decodeCube(cube);
		cube.markForRenderUpdate();
		for (TileEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}
	}
	
	public void handle(final PacketCubeBlockChange packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			return;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().theWorld;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			CubicChunks.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}

		ClientOpacityIndex index = (ClientOpacityIndex) cube.getColumn().getOpacityIndex();
		LightingManager lm = context.getLightingManager();
		for(int hmapUpdate : packet.heightValues) {
			int x = hmapUpdate & 0xF;
			int z = (hmapUpdate >> 4) & 0xF;
			//height is signed, so don't use unsigned shift
			int height = hmapUpdate >> 8;

			Integer oldHeight = index.getTopBlockY(x, z);
			index.setHeight(x, z, height);
			if(oldHeight == null || height == Integer.MIN_VALUE) {
				continue;
			}
			lm.updateSkyLightForBlockChange(cube.getColumn(), x + cubeX * 16, z + cubeZ * 16, oldHeight, height);
			//TODO: Optimize it. it's not always needed.
			lm.queueSkyLightOcclusionCalculation(x + cubeX * 16, z + cubeZ * 16);
		}
		// apply the update
		for (int i=0; i<packet.localAddresses.length; i++) {
			BlockPos pos = cube.localAddressToBlockPos(packet.localAddresses[i]);
			worldClient.invalidateRegionAndSetBlock(pos, packet.blockStates[i]);
			worldClient.checkLightFor(EnumSkyBlock.SKY, pos);
		}
		for (TileEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}	
	}
}
