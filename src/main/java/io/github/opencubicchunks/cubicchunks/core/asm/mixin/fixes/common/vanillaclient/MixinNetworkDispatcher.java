package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkDispatcher.class)
public class MixinNetworkDispatcher {
    @Shadow
    private EntityPlayerMP player;

    @Inject(
            method = "Lnet/minecraftforge/fml/common/network/handshake/NetworkDispatcher;write(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/network/internal/FMLProxyPacket;toS3FPackets()Ljava/util/List;",
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void dontSendForgePluginMessagesToVanillaClients(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, CallbackInfo ci) throws Exception {
        if (!VanillaNetworkHandler.hasFML(this.player)) {
            ci.cancel();
        }
    }
}
