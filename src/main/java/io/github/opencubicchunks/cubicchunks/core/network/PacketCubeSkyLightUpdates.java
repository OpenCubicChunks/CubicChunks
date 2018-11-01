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
package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import gnu.trove.list.TShortList;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Arrays;

import javax.annotation.Nullable;

public class PacketCubeSkyLightUpdates implements IMessage {

    private CubePos cube;
    private boolean isFullRelight;
    private byte[] data;

    public PacketCubeSkyLightUpdates() {
    }

    public PacketCubeSkyLightUpdates(Cube cube, TShortList updates) {
        if (cube.getStorage() == null) {
            // no light
            this.isFullRelight = true;
            this.data = null;
            return;
        }
        this.cube = cube.getCoords();
        this.data = new byte[updates.size() * 2];
        for (int i = 0; i < updates.size(); i++) {
            short packed = updates.get(i);
            int localX = AddressTools.getLocalX(packed);
            int localY = AddressTools.getLocalY(packed);
            int localZ = AddressTools.getLocalZ(packed);
            int value = cube.getStorage().getSkyLight(localX, localY, localZ);
            byte byte1 = (byte) (Bits.packUnsignedToInt(localX, 4, 0) | Bits.packUnsignedToInt(localY, 4, 4));
            byte byte2 = (byte) (Bits.packUnsignedToInt(localZ, 4, 0) | Bits.packUnsignedToInt(value, 4, 4));
            this.data[i * 2] = byte1;
            this.data[i * 2 + 1] = byte2;
        }
    }

    public PacketCubeSkyLightUpdates(Cube cube) {
        this.isFullRelight = true;
        if (cube.getStorage() == null) {
            // no light
            this.data = null;
            return;
        }
        this.cube = cube.getCoords();
        this.data = Arrays.copyOf(cube.getStorage().getSkyLight().getData(), Cube.SIZE * Cube.SIZE * Cube.SIZE / 2);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.cube = new CubePos(buf.readInt(), buf.readInt(), buf.readInt());
        this.isFullRelight = buf.readBoolean();
        boolean hasData = buf.readBoolean();
        if (hasData) {
            int size = ByteBufUtils.readVarInt(buf, 3);
            this.data = new byte[size];
            buf.readBytes(this.data);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.cube.getX());
        buf.writeInt(this.cube.getY());
        buf.writeInt(this.cube.getZ());

        buf.writeBoolean(this.isFullRelight);
        buf.writeBoolean(this.data != null);

        if (this.data != null) {
            ByteBufUtils.writeVarInt(buf, this.data.length, 3);
            buf.writeBytes(this.data);
        }
    }

    CubePos getCubePos() {
        return cube;
    }

    boolean isFullRelight() {
        return isFullRelight;
    }

    byte[] getData() {
        return data;
    }

    public int updateCount() {
        return data.length / 2;
    }

    public static class Handler extends AbstractClientMessageHandler<PacketCubeSkyLightUpdates> {

        @Nullable @Override
        public IMessage handleClientMessage(EntityPlayer player, PacketCubeSkyLightUpdates message, MessageContext ctx) {
            ClientHandler.getInstance().handle(message);
            return null;
        }
    }
}