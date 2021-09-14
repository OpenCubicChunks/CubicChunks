package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.placement.CubicHeightProvider;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(UniformHeight.class)
public abstract class MixinUniformHeightProvider implements CubicHeightProvider {

    @Shadow @Final private VerticalAnchor maxInclusive;

    @Shadow @Final private VerticalAnchor minInclusive;

    @Override public OptionalInt sampleCubic(Random rand, WorldGenerationContext context, int cubeMinY, int cubeMaxY) {
        int minHeight = this.minInclusive.resolveY(context);
        double bottomOffset = minHeight == (((DecorationContext) context).getLevel()).getLevel().dimensionType().minY() ? Double.NEGATIVE_INFINITY : minHeight;

        int maxHeight = this.maxInclusive.resolveY(context);
        double probability = 1.0 / ((maxHeight - minHeight + 1) / (256.0 / CubeAccess.DIAMETER_IN_BLOCKS));

        if (rand.nextDouble() > probability) {
            return OptionalInt.empty();
        }

        double maxBlockY = maxHeight + 1;
        int blockY = rand.nextInt(cubeMaxY - cubeMinY + 1) + cubeMinY/*Equivalent to cubepos.getMinBlockY in 1.12*/;

        if (blockY > maxBlockY || blockY < bottomOffset) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(blockY);
    }
}
