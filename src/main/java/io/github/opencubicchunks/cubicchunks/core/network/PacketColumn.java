/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketColumn implements IMessage {

    private ChunkPos chunkPos;
    private byte[] data;

    public PacketColumn() {
    }

    public PacketColumn(Chunk column) {
        this.chunkPos = column.getPos();
        this.data = new byte[WorldEncoder.getEncodedSize(column)];
        PacketBuffer out = new PacketBuffer(WorldEncoder.createByteBufForWrite(this.data));

        WorldEncoder.encodeColumn(out, column);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
        this.data = new byte[buf.readInt()];
        buf.readBytes(this.data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(chunkPos.x);
        buf.writeInt(chunkPos.z);
        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);
    }

    ChunkPos getChunkPos() {
        return chunkPos;
    }

    byte[] getData() {
        return data;
    }

    public static class Handler extends AbstractClientMessageHandler<PacketColumn> {

        @Nullable @Override
        public void handleClientMessage(World world, EntityPlayer player, PacketColumn packet, MessageContext ctx) {
            ICubicWorld worldClient = (ICubicWorld) world;
            CubeProviderClient cubeCache = (CubeProviderClient) worldClient.getCubeCache();

            ChunkPos chunkPos = packet.getChunkPos();

            Chunk column = cubeCache.loadChunk(chunkPos.x, chunkPos.z);

            byte[] data = packet.getData();
            ByteBuf buf = WorldEncoder.createByteBufForRead(data);

            WorldEncoder.decodeColumn(new PacketBuffer(buf), column);
        }
    }
}
