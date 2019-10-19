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

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketUnloadColumn implements IMessage {

    private ChunkPos chunkPos;

    public PacketUnloadColumn() {
    }

    public PacketUnloadColumn(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(chunkPos.x);
        buf.writeInt(chunkPos.z);
    }

    ChunkPos getColumnPos() {
        return Preconditions.checkNotNull(chunkPos);
    }

    public static class Handler extends AbstractClientMessageHandler<PacketUnloadColumn> {

        @Nullable @Override
        public void handleClientMessage(World world, EntityPlayer player, PacketUnloadColumn message, MessageContext ctx) {
            ICubicWorld worldClient = (ICubicWorld) world;
            if (!worldClient.isCubicWorld()) {
                // Workaround for vanilla: when going between dimensions, chunk unload packets are received for the old dimension
                // are received when client already has the new dimension. In vanilla it just happens to cause no issues but it breaks cubic chunks
                // if we don't check for it
                return;
            }
            CubeProviderClient cubeCache = (CubeProviderClient) worldClient.getCubeCache();

            ChunkPos chunkPos = message.getColumnPos();
            cubeCache.unloadChunk(chunkPos.x, chunkPos.z);
        }
    }
}
