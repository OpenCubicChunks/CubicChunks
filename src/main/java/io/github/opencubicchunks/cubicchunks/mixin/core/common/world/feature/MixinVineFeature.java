package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.VinesFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VinesFeature.class)
public class MixinVineFeature {

    @Inject(at = @At("HEAD"),
        method = "place",
        cancellable = true)
    private void cancelVines(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) featurePlaceContext.level()).isCubic()) {
            return;
        }

        cir.cancel();
    }
}
