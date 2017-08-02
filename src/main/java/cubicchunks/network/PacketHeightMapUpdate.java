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

import com.google.common.base.Preconditions;
import cubicchunks.util.AddressTools;
import cubicchunks.world.IHeightMap;
import gnu.trove.list.TByteList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class PacketHeightMapUpdate implements IMessage {

    private ChunkPos chunk;
    private TByteList updates;
    private TIntList heights;

    public PacketHeightMapUpdate() {
    }

    public PacketHeightMapUpdate(ChunkPos chunk, TByteList updates, IHeightMap heightMap) {
        this.chunk = chunk;
        this.updates = new TByteArrayList();
        this.heights = new TIntArrayList();
        for (int i = 0; i < updates.size(); i++) {
            byte pos = updates.get(i);
            if (this.updates.contains(pos)) {
                continue;
            }
            this.updates.add(pos);
            this.heights.add(heightMap.getTopBlockY(AddressTools.getLocalX(pos), AddressTools.getLocalZ(pos)));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunk = new ChunkPos(buf.readInt(), buf.readInt());

        int size = buf.readUnsignedByte();
        this.updates = new TByteArrayList(size);
        this.heights = new TIntArrayList(size);

        for (int i = 0; i < size; i++) {
            this.updates.add(buf.readByte());
            this.heights.add(ByteBufUtils.readVarInt(buf, 5));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunk.chunkXPos);
        buf.writeInt(this.chunk.chunkZPos);

        buf.writeByte(this.updates.size());

        for (int i = 0; i < this.updates.size(); i++) {
            buf.writeByte(this.updates.get(i) & 0xFF);
            ByteBufUtils.writeVarInt(buf, this.heights.get(i), 5);
        }
    }

    ChunkPos getColumnPos() {
        return Preconditions.checkNotNull(this.chunk);
    }

    TByteList getUpdates() {
        return this.updates;
    }

    TIntList getHeights() {
        return this.heights;
    }

    public static class Handler extends AbstractClientMessageHandler<PacketHeightMapUpdate> {

        @Nullable @Override
        public IMessage handleClientMessage(EntityPlayer player, PacketHeightMapUpdate message, MessageContext ctx) {
            ClientHandler.getInstance().handle(message);
            return null;
        }
    }
}