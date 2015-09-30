/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.network;

import net.minecraft.network.INetHandler;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.BlockPos;
import cubicchunks.TallWorldsMod;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.util.AddressTools;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;

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
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
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
			TallWorldsMod.LOGGER.error("Ignored {}/{} cubes that arrived before their columns", outOfOrderCubes, packet.cubeAddresses.length);
		}
		
		packet.finishDecoding();
		
		// update column metadata
		for (long columnAddress : packet.columnAddresses) {
			Column column = cubeCache.getColumn(AddressTools.getX(columnAddress), AddressTools.getZ(columnAddress));
			
			// update lighting flags
			if (! (worldClient.dimension instanceof DimensionOverworld)) {
				column.resetRelightChecks();
			}
			column.terrainPopulated = true;
			
			// update tile entities in each chunk
			for (Cube cube : column.getCubes()) {
				for (BlockEntity blockEntity : cube.getBlockEntities()) {
					blockEntity.updateContainingBlockInfo();
				}
			}
		}
	}
	
	public void handle(final PacketUnloadCubes packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
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
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
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
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			TallWorldsMod.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// apply the update
		packet.decodeCube(cube);
		cube.markForRenderUpdate();
		for (BlockEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}
	}
	
	public void handle(final PacketCubeBlockChange packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			TallWorldsMod.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// apply the update
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i=0; i<packet.localAddresses.length; i++) {
			cube.setBlockState(cube.localAddressToBlockPos(pos, packet.localAddresses[i]), packet.blockStates[i]);
		}
		cube.markForRenderUpdate();
		for (BlockEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}	
	}
}
