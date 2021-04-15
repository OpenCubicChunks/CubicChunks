package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.world.gen.placement.CubicHeightProvider;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HeightProvider.class)
public class MixinHeightProvider implements CubicHeightProvider {

    @Override public OptionalInt sampleCubic(Random random, WorldGenerationContext context, int minY, int maxY) {
        return OptionalInt.empty();
    }
}
