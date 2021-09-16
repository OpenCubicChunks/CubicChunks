package io.github.opencubicchunks.cubicchunks.mixin.core.client.server;

import io.github.opencubicchunks.cubicchunks.chunk.VerticalViewDistanceListener;
import net.minecraft.client.server.IntegratedPlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IntegratedPlayerList.class)
public class MixinIntegratedPlayerList {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedPlayerList;setViewDistance(I)V"))
    private void setVerticalViewDistance(IntegratedPlayerList integratedPlayerList, int viewDistance) {
        ((VerticalViewDistanceListener) integratedPlayerList).setIncomingVerticalViewDistance(viewDistance);
        ((IntegratedPlayerList) (Object) this).setViewDistance(viewDistance);
    }
}
