package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement.verticalanchor;

import io.github.opencubicchunks.cubicchunks.levelgen.carver.CubicCarvingContext;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.VerticalAnchorAccess;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VerticalAnchor.AboveBottom.class)
public class MixinVerticalAnchorAboveBottom {

    @Inject(method = "resolveY", at = @At("HEAD"), cancellable = true)
    private void resolveCubicChunksY(WorldGenerationContext context, CallbackInfoReturnable<Integer> cir) {
        if (context instanceof CubicCarvingContext) {
            int defaultValue = ((CubicCarvingContext) context).getOriginalMinGenY() + ((VerticalAnchorAccess) this).invokeValue();
            cir.setReturnValue(defaultValue);
        }
    }
}
