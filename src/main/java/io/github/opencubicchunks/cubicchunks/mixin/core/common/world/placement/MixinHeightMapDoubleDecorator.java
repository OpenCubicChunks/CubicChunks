package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import io.github.opencubicchunks.cubicchunks.chunk.util.CCMathUtils;
import net.minecraft.world.level.levelgen.placement.HeightmapDoubleDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(HeightmapDoubleDecorator.class)
public abstract class MixinHeightMapDoubleDecorator {

    @Redirect(method = "getPositions", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int allowNegativeCoords(Random rand, int bound) {
        return CCMathUtils.getRandomPositiveOrNegativeY(rand, bound);
    }
}
