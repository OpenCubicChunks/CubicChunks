package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Enable this
@Mixin(SpringFeature.class)
public class MixinSpringFeature {

    @Inject(at = @At("HEAD"), method = "place", cancellable = true)
    private void cancel(FeaturePlaceContext<SpringConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) featurePlaceContext.level()).isCubic()) {
            return;
        }

        cir.setReturnValue(true);
    }
}
