package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {
    @Redirect(method = "sendChunkLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket"
            + "(Lnet/minecraft/network/IPacket;)V", ordinal = 0))
    public void onSendChunkLoad(ServerPlayNetHandler serverPlayNetHandler, IPacket<?> packetIn) { }
}
