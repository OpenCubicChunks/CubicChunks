package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.carver;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.levelgen.util.NonAtomicWorldgenRandom;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldCarver.class)
public class MixinWorldCarver {

    @Redirect(method = "carveEllipsoid", at = @At(value = "NEW", target = "java/util/Random", remap = false))
    private Random createRandom(long seed) {
        return new NonAtomicWorldgenRandom(seed);
    }

    @Redirect(method = "getCarveState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/VerticalAnchor;resolveY(Lnet/minecraft/world/level/levelgen/WorldGenerationContext;)I"))
    private int returnMinIntValue(VerticalAnchor verticalAnchor, WorldGenerationContext context) {
        return CubicChunks.MIN_SUPPORTED_HEIGHT;
    }
}
