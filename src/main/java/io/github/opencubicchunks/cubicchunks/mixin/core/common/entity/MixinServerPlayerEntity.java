package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntity {
    @Redirect(method = "trackChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    public void onSendChunkLoad(ServerGamePacketListenerImpl serverPlayNetHandler, Packet<?> packetIn) { }
}