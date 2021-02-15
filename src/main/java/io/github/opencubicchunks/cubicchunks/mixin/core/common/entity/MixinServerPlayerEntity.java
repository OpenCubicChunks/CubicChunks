package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity {
    @Shadow public abstract ServerLevel getLevel();

    @Redirect(method = "trackChunk",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    public void onSendChunkLoad(ServerGamePacketListenerImpl serverPlayNetHandler, Packet<?> packetIn) {
        if (!((CubicLevelHeightAccessor) this.getLevel()).isCubic()) {
            serverPlayNetHandler.send(packetIn);
        }
    }
}