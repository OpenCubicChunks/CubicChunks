package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.placement.CubicHeightProvider;
import io.github.opencubicchunks.cubicchunks.levelgen.placement.PeriodicUserFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TrapezoidHeight.class)
public abstract class MixinTrapezoidHeightProvider implements CubicHeightProvider {

    @Shadow @Final private VerticalAnchor maxInclusive;

    @Shadow @Final private VerticalAnchor minInclusive;

    @Shadow @Final private int plateau;

    private PeriodicUserFunction userFunction;

    @Override public OptionalInt sampleCubic(Random rand, WorldGenerationContext context, int cubeMinY, int cubeMaxY) {
        int minHeight = this.minInclusive.resolveY(context) - 1;
        int maxHeight = this.maxInclusive.resolveY(context) + 1;

        int plateauStart = (minHeight + maxHeight - plateau) / 2;
        int plateauEnd = (minHeight + maxHeight + plateau) / 2;

        float plateauProbability = 2.0F / (plateau + maxHeight - minHeight);
        if (userFunction == null) {
            userFunction = new PeriodicUserFunction.Builder().point(minHeight, 0).point(maxHeight, 0).point(plateauStart, plateauProbability)
                .point(plateauEnd, plateauProbability).repeatRange(minHeight - context.getGenDepth() / 2.0F, maxHeight + context.getGenDepth() / 2.0F).build();
        }
        float randomFloat = rand.nextFloat();
        float counter = 0.0F;

        for (int cubeY = cubeMinY; cubeY <= cubeMaxY; cubeY++) {
            counter += userFunction.getValue(cubeY);
            if (counter >= randomFloat) {
                //TODO: Is there a better way of not capturing higher y level ores and repeating them?
                if ((minHeight + maxHeight) / 2 > 128 && cubeY < minHeight) {
                    return OptionalInt.empty();
                }

                return OptionalInt.of(cubeY);
            }
        }

        return OptionalInt.empty();
    }
}
