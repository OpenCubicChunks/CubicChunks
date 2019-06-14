package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkPrimer.class)
public class MixinChunkPrimer {
    @Redirect(method = {"getBlockState", "getFluidState"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isYOutOfBounds(I)Z"))
    private boolean isYOutOfBounds(int y) {
        return y < 0 || y > 255;
    }
}
