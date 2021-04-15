package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature.range;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.GlowLichenFeature;
import net.minecraft.world.level.levelgen.feature.SmallDripstoneFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmallDripstoneFeature.class)
public class MixinDripstoneFeature {


    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void cancelPlacement(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
