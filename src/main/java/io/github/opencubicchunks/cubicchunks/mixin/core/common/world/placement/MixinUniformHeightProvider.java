package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.world.gen.placement.CubicHeightProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(UniformHeight.class)
public abstract class MixinUniformHeightProvider implements CubicHeightProvider {

    @Shadow @Final private VerticalAnchor maxInclusive;

    @Shadow @Final private VerticalAnchor minInclusive;

    @Override public OptionalInt sampleCubic(Random rand, WorldGenerationContext context, int cubeMinY, int maxY) {
        int minInclusive = this.minInclusive.resolveY(context);
        double bottomOffset = minInclusive == 0 ? Double.NEGATIVE_INFINITY : minInclusive;

        int maxInclusive = this.maxInclusive.resolveY(context);
        double probability = 1.0 / ((maxInclusive - minInclusive + 1) / (256.0 / IBigCube.DIAMETER_IN_BLOCKS));

        if (rand.nextDouble() > probability) {
            return OptionalInt.empty();
        }

        double maxBlockY = maxInclusive + 1;
        int blockY = rand.nextInt(maxY - cubeMinY) + cubeMinY/*Equivalent to cubepos.getMinBlockY in 1.12*/;

        if (blockY > maxBlockY || blockY < bottomOffset) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(blockY);
    }
}
