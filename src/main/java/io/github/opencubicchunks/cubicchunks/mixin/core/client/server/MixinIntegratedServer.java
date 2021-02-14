package io.github.opencubicchunks.cubicchunks.mixin.core.client.server;

import java.util.function.BooleanSupplier;

import io.github.opencubicchunks.cubicchunks.chunk.IVerticalView;
import io.github.opencubicchunks.cubicchunks.client.IVerticalViewDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {


    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), cancellable = true)
    private void tickServer(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        int horizontalViewDistance = Math.max(2, this.minecraft.options.renderDistance + -1);

        int verticalViewDistance = Math.max(2, ((IVerticalViewDistance) this.minecraft.options).getVerticalViewDistance() + -1);
        int currentVerticalViewDistance1 = ((IVerticalView) ((IntegratedServer) (Object) this).getPlayerList()).getVerticalViewDistance();

        if (verticalViewDistance != currentVerticalViewDistance1) {
            LOGGER.info("Changing vertical view distance to {}, from {}", verticalViewDistance, currentVerticalViewDistance1);
            ((IVerticalView) ((IntegratedServer) (Object) this).getPlayerList()).setCubeViewDistance(horizontalViewDistance, verticalViewDistance);
        }
    }
}
