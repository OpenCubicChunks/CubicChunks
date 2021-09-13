package io.github.opencubicchunks.cubicchunks.levelgen;

import java.util.Random;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public interface CubicSimpleFeatureDecorator<DC extends DecoratorConfiguration> {

    default Stream<BlockPos> placeCubic(Random rand, DC config, BlockPos minCubePos) {
        throw new UnsupportedOperationException("Not Implemented for " + this.getClass());
    }
}
