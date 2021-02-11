package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave;

import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Aquifer.class)
public class MixinAquifer {


    @Inject(method = "computeAt", at = @At("HEAD"), cancellable = true)
    private void cancelAquiferComputing(int i, int j, int k, CallbackInfo ci) {
        ci.cancel();
    }
}
