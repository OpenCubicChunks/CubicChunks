package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IVanillaEncodingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
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
