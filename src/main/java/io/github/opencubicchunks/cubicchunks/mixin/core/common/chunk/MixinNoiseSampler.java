package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import net.minecraft.world.level.levelgen.NoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseSampler.class)
public class MixinNoiseSampler {


    @Inject(method = "applySlide", at = @At("HEAD"), cancellable = true)
    private void dontSlideInfiniteCubeGen(double noise, int y, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(noise);
    }
}
