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
import cubicchunks.client.CubeProviderClient;
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Bits;
import cubicchunks.world.ClientHeightMap;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("MethodCallSideOnly")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ClientHandler implements INetHandler {

    private static final ClientHandler m_instance = new ClientHandler();
    
    public static ClientHandler getInstance() {
        return m_instance;
    }

    @Override
    public void onDisconnect(ITextComponent chat) {
        // nothing to do
    }

    public void handle(PacketColumn packet) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handle(packet));
            return;
        }

        ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().world;
        CubeProviderClient cubeCache = worldClient.getCubeCache();

        ChunkPos chunkPos = packet.getChunkPos();

        IColumn column = (IColumn) cubeCache.loadChunk(chunkPos.x, chunkPos.z);

        byte[] data = packet.getData();
        ByteBuf buf = WorldEncoder.createByteBufForRead(data);

        WorldEncoder.decodeColumn(new PacketBuffer(buf), column);
    }

    public void handle(final PacketUnloadColumn packet) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handle(packet));
            return;
        }

        ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().world;
        CubeProviderClient cubeCache = worldClient.getCubeCache();

        ChunkPos chunkPos = packet.getColumnPos();
        cubeCache.unloadChunk(chunkPos.x, chunkPos.z);
    }

    public void handle(final PacketCubeBlockChange packet) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handle(packet));
            return;
        }

        ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().world;
        CubeProviderClient cubeCache = worldClient.getCubeCache();

        // get the cube
        Cube cube = cubeCache.getCube(packet.cubePos);
        if (cube instanceof BlankCube) {
            CubicChunks.LOGGER.error("Ignored block update to blank cube {}", packet.cubePos);
            return;
        }

        ClientHeightMap index = (ClientHeightMap) cube.getColumn().getOpacityIndex();
        for (int hmapUpdate : packet.heightValues) {
            int x = hmapUpdate & 0xF;
            int z = (hmapUpdate >> 4) & 0xF;
            //height is signed, so don't use unsigned shift
            int height = hmapUpdate >> 8;
            index.setHeight(x, z, height);
        }
        // apply the update
        for (int i = 0; i < packet.localAddresses.length; i++) {
            BlockPos pos = cube.localAddressToBlockPos(packet.localAddresses[i]);
            worldClient.invalidateRegionAndSetBlock(pos, packet.blockStates[i]);
        }
        cube.getTileEntityMap().values().forEach(TileEntity::updateContainingBlockInfo);
    }

    public void handle(PacketHeightMapUpdate message) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handle(message));
            return;
        }
        ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().world;
        CubeProviderClient cubeCache = worldClient.getCubeCache();

        int columnX = message.getColumnPos().x;
        int columnZ = message.getColumnPos().z;

        IColumn column = cubeCache.provideColumn(columnX, columnZ);
        if (column instanceof EmptyChunk) {
            CubicChunks.LOGGER.error("Ignored block update to blank column {}", message.getColumnPos());
            return;
        }

        ClientHeightMap index = (ClientHeightMap) column.getOpacityIndex();
        LightingManager lm = worldClient.getLightingManager();

        int size = message.getUpdates().size();

        for (int i = 0; i < size; i++) {
            int packed = message.getUpdates().get(i) & 0xFF;
            int x = AddressTools.getLocalX(packed);
            int z = AddressTools.getLocalZ(packed);
            int height = message.getHeights().get(i);

            int oldHeight = index.getTopBlockY(x, z);
            index.setHeight(x, z, height);
            // Disable due to huge client side performance loss on accepting freshly generated cubes light updates.
            // More info at  https://github.com/OpenCubicChunks/CubicChunks/pull/328
            //lm.onHeightMapUpdate(column, x, z, oldHeight, height);
        }
    }

    public void handle(PacketCubeSkyLightUpdates message) {
        IThreadListener taskQueue = Minecraft.getMinecraft();
        if (!taskQueue.isCallingFromMinecraftThread()) {
            taskQueue.addScheduledTask(() -> handle(message));
            return;
        }
        ICubicWorldClient worldClient = (ICubicWorldClient) Minecraft.getMinecraft().world;
        CubeProviderClient cubeCache = worldClient.getCubeCache();

        // get the cube
        Cube cube = cubeCache.getCube(message.getCubePos());
        if (message.getData() == null) {
            // this means the EBS was null serverside. So it needs to be null clientside
            cube.setStorage(Chunk.NULL_BLOCK_STORAGE);
            return;
        }
        ExtendedBlockStorage storage = cube.getStorage();
        if (cube.getStorage() == null) {
            cube.setStorage(storage = new ExtendedBlockStorage(cube.getY(), worldClient.getProvider().hasSkyLight()));
        }
        assert storage != null;
        if (message.isFullRelight()) {
            storage.setSkyLight(new NibbleArray(message.getData()));
        } else {
            for (int i = 0; i < message.updateCount(); i++) {
                int packed1 = message.getData()[i * 2] & 0xFF;
                int packed2 = message.getData()[i * 2 + 1] & 0xFF;
                storage.setSkyLight(Bits.unpackUnsigned(packed1, 4, 0), Bits.unpackUnsigned(packed1, 4, 4),
                        Bits.unpackUnsigned(packed2, 4, 0), Bits.unpackUnsigned(packed2, 4, 4));
            }
        }
        LightingManager.CubeLightUpdateInfo info = cube.getCubeLightUpdateInfo();
        if (info != null) {
            info.clear();
        }
        cube.markForRenderUpdate();
    }
}
