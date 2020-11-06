package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Redirect(method = {"handleForgetLevelChunk", "handleLevelChunk"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMaxSection()I"))
    private int getFakeMaxSectionY(ClientLevel clientLevel) {
        return clientLevel.getMinSection() - 1; // disable the loop, cube packets do the necessary work
    }
}
