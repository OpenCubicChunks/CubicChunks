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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IVanillaEncodingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NettyPacketEncoder.class)
public class MixinNettyPacketEncoder {
    @Redirect(
            method = "Lnet/minecraft/network/NettyPacketEncoder;encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Packet;writePacketData(Lnet/minecraft/network/PacketBuffer;)V"))
    private void encodePacketAsVanillaIfNeeded(Packet<?> packet, PacketBuffer buf, ChannelHandlerContext ctx, Packet<?> packetAgain, ByteBuf bufAgain) throws Exception {
        if (packet instanceof IVanillaEncodingPacket)   { //the packet has indicated that it may be encoded as a vanilla packet
            //this cast is safe, we assume that only clientbound packets during PLAY implement IVanillaEncodingPacket
            NetHandlerPlayServer handler = (NetHandlerPlayServer) ctx.channel().attr(NetworkDispatcher.FML_DISPATCHER).get().manager.getNetHandler();

            //encode packet
            ((IVanillaEncodingPacket) packet).writeVanillaPacketData(buf, handler.player);
            return;
        }

        packet.writePacketData(buf);
    }
}
