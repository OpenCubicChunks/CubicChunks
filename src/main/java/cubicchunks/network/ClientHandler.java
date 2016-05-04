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

import com.google.common.base.Throwables;
import cubicchunks.CubicChunks;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.lighting.LightingManager;
import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

import java.io.IOException;

import static cubicchunks.util.AddressTools.*;

public class ClientHandler implements INetHandler {

	private static ClientHandler m_instance;

	public static ClientHandler getInstance() {
		if (m_instance == null) {
			m_instance = new ClientHandler();
		}
		return m_instance;
	}

	@Override
	public void onDisconnect(ITextComponent chat) {
		// nothing to do
	}

	public void handle(PacketCube packet) {
		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}
		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();

		long cubeAddress = packet.getCubeAddress();

		int cubeX = getX(cubeAddress);
		int cubeY = getY(cubeAddress);
		int cubeZ = getZ(cubeAddress);

		Column column = cubeCache.getColumn(cubeX, cubeZ);
		//isEmpty actually checks if the column is a BlankColumn
		if (column.isEmpty()) {
			CubicChunks.LOGGER.error("Out of order cube received! No column for cube at ({}, {}, {}) exists!", cubeX, cubeY, cubeZ);
			return;
		}
		Cube cube = column.getOrCreateCube(cubeY, false);
		byte[] data = packet.getData();
		ByteBuf buf = WorldEncoder.createByteBufForRead(data);
		try {
			WorldEncoder.decodeCube(new PacketBuffer(buf), cube);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
		cube.markForRenderUpdate();
	}

	public void handle(PacketColumn packet) {
		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}
		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();

		long cubeAddress = packet.getCubeAddress();

		int cubeX = getX(cubeAddress);
		int cubeZ = getZ(cubeAddress);

		Column column = cubeCache.loadChunk(cubeX, cubeZ);

		byte[] data = packet.getData();
		ByteBuf buf = WorldEncoder.createByteBufForRead(data);
		try {
			WorldEncoder.decodeColumn(new PacketBuffer(buf), column);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	public void handle(final PacketUnloadCube packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}

		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();
		long cubeAddress = packet.getCubeAddress();
		cubeCache.unloadCube(
				getX(cubeAddress),
				getY(cubeAddress),
				getZ(cubeAddress)
		);

	}

	public void handle(final PacketUnloadColumn packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}

		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();

		long cubeAddress = packet.getCubeAddress();
		cubeCache.unloadColumn(
				getX(cubeAddress),
				getZ(cubeAddress)
		);
	}

	public void handle(final PacketCubeChange packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}

		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();

		// get the cube
		int cubeX = getX(packet.cubeAddress);
		int cubeY = getY(packet.cubeAddress);
		int cubeZ = getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			CubicChunks.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}

		// apply the update
		packet.decodeCube(cube);
		cube.markForRenderUpdate();
		for (TileEntity blockEntity : cube.getTileEntityMap()) {
			blockEntity.updateContainingBlockInfo();
		}
	}

	public void handle(final PacketCubeBlockChange packet) {

		IThreadListener taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isCallingFromMinecraftThread()) {
			taskQueue.addScheduledTask(() -> handle(packet));
			return;
		}

		ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().theWorld;
		ClientCubeCache cubeCache = worldClient.getCubeCache();

		// get the cube
		int cubeX = getX(packet.cubeAddress);
		int cubeY = getY(packet.cubeAddress);
		int cubeZ = getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			CubicChunks.LOGGER.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}

		ClientOpacityIndex index = (ClientOpacityIndex) cube.getColumn().getOpacityIndex();
		LightingManager lm = worldClient.getLightingManager();
		for (int hmapUpdate : packet.heightValues) {
			int x = hmapUpdate & 0xF;
			int z = (hmapUpdate >> 4) & 0xF;
			//height is signed, so don't use unsigned shift
			int height = hmapUpdate >> 8;

			Integer oldHeight = index.getTopBlockY(x, z);
			index.setHeight(x, z, height);
			if (oldHeight == null || height == Integer.MIN_VALUE) {
				continue;
			}

			int minY = Math.min(oldHeight, height);
			int maxY = Math.max(oldHeight, height);
			lm.columnSkylightUpdate(LightingManager.UpdateType.QUEUED, cube.getColumn(), x, minY, maxY, z);
		}
		// apply the update
		for (int i = 0; i < packet.localAddresses.length; i++) {
			BlockPos pos = cube.localAddressToBlockPos(packet.localAddresses[i]);
			worldClient.invalidateRegionAndSetBlock(pos, packet.blockStates[i]);
		}
		for (TileEntity blockEntity : cube.getTileEntityMap()) {
			blockEntity.updateContainingBlockInfo();
		}
	}
}
