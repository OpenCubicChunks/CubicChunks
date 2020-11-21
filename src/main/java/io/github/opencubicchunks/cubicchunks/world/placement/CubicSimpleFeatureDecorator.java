package io.github.opencubicchunks.cubicchunks.world.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

import java.util.Random;
import java.util.stream.Stream;

public interface CubicSimpleFeatureDecorator<DC extends DecoratorConfiguration> {

    default Stream<BlockPos> placeCubic(Random rand, DC config, BlockPos minCubePos) {
        throw new UnsupportedOperationException("Not Implemented for " + this.getClass());
    }
}
