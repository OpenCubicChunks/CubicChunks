package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import java.util.Random;

import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//TODO: Handle this when heightproviders decorators(ores) are finalized
@Mixin(value = UniformHeight.class)
public abstract class MixinUniformHeight {

    @Shadow @Final private VerticalAnchor minInclusive;

    @Shadow @Final private VerticalAnchor maxInclusive;

    @Inject(method = "sample", at = @At("HEAD"), cancellable = true)
    private void shutupLogger(Random random, WorldGenerationContext worldGenerationContext, CallbackInfoReturnable<Integer> cir) {
        int min = this.minInclusive.resolveY(worldGenerationContext);
        int max = this.maxInclusive.resolveY(worldGenerationContext);
        if (min >= max) {
            cir.setReturnValue(min);
        } else {
            cir.setReturnValue(min);
        }
    }
}
