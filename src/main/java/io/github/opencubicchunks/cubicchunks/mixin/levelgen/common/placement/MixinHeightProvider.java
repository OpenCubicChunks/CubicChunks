package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.placement.CubicHeightProvider;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HeightProvider.class)
public abstract class MixinHeightProvider implements CubicHeightProvider {

    @Shadow public abstract int sample(Random random, WorldGenerationContext context);

    @Override public OptionalInt sampleCubic(Random random, WorldGenerationContext context, int minY, int maxY) {
        int sampledY = this.sample(random, context);
        if (sampledY <= maxY && sampledY >= minY) {
            return OptionalInt.of(sampledY);
        }
        return OptionalInt.empty();
    }
}
