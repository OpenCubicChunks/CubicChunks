package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.CubicChunks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {
    @Inject(at = @At("RETURN"), method = "isYOutOfBounds", cancellable = true)
    private static void isYOutOfBounds(int y, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(y < -512 || y >= CubicChunks.worldMAXHeight);
    }

}
