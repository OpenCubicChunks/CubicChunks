/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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

import com.google.common.base.Preconditions;
import gnu.trove.list.TByteList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.BitSet;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketHeightMapUpdate implements IMessage {

    private ChunkPos chunk;
    private TByteList updates;
    private TIntList heights;

    public PacketHeightMapUpdate() {
    }

    public PacketHeightMapUpdate(BitSet updates, Chunk chunk) {
        this.chunk = chunk.getPos();
        this.updates = new TByteArrayList();
        this.heights = new TIntArrayList();
        for (int i = updates.nextSetBit(0); i >= 0; i = updates.nextSetBit(i + 1)) {
            this.updates.add((byte) i);
            this.heights.add(((IColumnInternal) chunk).getHeightWithStaging(AddressTools.getLocalX(i), AddressTools.getLocalZ(i)) - 1);
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
        buf.writeInt(this.chunk.x);
        buf.writeInt(this.chunk.z);

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

        @Override
        public void handleClientMessage(World world, EntityPlayer player, PacketHeightMapUpdate message, MessageContext ctx) {
            ICubicWorldInternal.Client worldClient = (ICubicWorldInternal.Client) world;
            CubeProviderClient cubeCache = worldClient.getCubeCache();

            int columnX = message.getColumnPos().x;
            int columnZ = message.getColumnPos().z;

            Chunk column = cubeCache.provideColumn(columnX, columnZ);
            if (column instanceof EmptyChunk) {
                CubicChunks.LOGGER.error("Ignored block update to blank column {}", message.getColumnPos());
                return;
            }

            ClientHeightMap index = (ClientHeightMap) ((IColumn) column).getOpacityIndex();
            ILightingManager lm = worldClient.getLightingManager();

            int size = message.getUpdates().size();

            for (int i = 0; i < size; i++) {
                int packed = message.getUpdates().get(i) & 0xFF;
                int x = AddressTools.getLocalX(packed);
                int z = AddressTools.getLocalZ(packed);
                int height = message.getHeights().get(i);

                int oldHeight = index.getTopBlockY(x, z);
                index.setHeight(x, z, height);
                lm.updateLightBetween(column, x, oldHeight, height, z);
            }
        }
    }
}
