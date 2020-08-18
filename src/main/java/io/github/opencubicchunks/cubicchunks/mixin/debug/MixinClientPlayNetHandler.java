package io.github.opencubicchunks.cubicchunks.mixin.debug;

import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.play.server.SChunkDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class MixinClientPlayNetHandler {

    @Shadow private ClientWorld world;

    @Inject(
        method = "handleChunkData",
        at = @At("HEAD"),
        cancellable = true
    )
    private void guard$handleChunkData(SChunkDataPacket packetIn, CallbackInfo ci) {
        if (world == null)
            ci.cancel();
    }

}
