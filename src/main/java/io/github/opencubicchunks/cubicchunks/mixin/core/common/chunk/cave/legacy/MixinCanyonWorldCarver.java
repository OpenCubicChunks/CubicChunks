package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CanyonWorldCarver.class)
public class MixinCanyonWorldCarver {

    @Redirect(method = "skip", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/carver/CanyonWorldCarver;rs:[F"))
    private float cancelCanyons(float[] arg0, int idx) {
        return arg0[idx & 1023];
    }
}
