package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class MixinAquifer {


    @Inject(method = "isLavaLevel", at = @At("HEAD"), cancellable = true)
    private void isLavaLevel(int y, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(y <= CubicChunks.MIN_SUPPORTED_HEIGHT + 12);
    }

    @ModifyConstant(method = "computeAquifer", constant = @Constant(intValue = -20))
    private int alwaysTrue(int arg0, int x, int y, int z) {
        return Math.floorDiv(y, 40) * 40 + 20;
    }

    /**
     * @reason optimization: shift is faster than floorDiv
     * @author Gegy
     */
    @Overwrite
    private int gridX(int x) {
        return x >> 4;
    }

    /**
     * @reason optimization: shift is faster than floorDiv
     * @author Gegy
     */
    @Overwrite
    private int gridZ(int z) {
        return z >> 4;
    }
}

