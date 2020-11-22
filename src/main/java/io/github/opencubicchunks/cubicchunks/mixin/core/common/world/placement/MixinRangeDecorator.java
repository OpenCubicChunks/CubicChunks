package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.world.placement.CubicSimpleFeatureDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.RangeDecorator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RangeDecorator.class)
public abstract class MixinRangeDecorator<DC extends RangeDecoratorConfiguration> implements CubicSimpleFeatureDecorator<DC> {

    @Override
    public Stream<BlockPos> placeCubic(Random rand, DC config, BlockPos minCubePos) {

        int maximum = config.maximum;
        double bottomOffset = config.bottomOffset == 0 ? Double.NEGATIVE_INFINITY : config.bottomOffset;
        int topOffset = config.topOffset;

        int x = minCubePos.getX();
        int z = minCubePos.getZ();

        double probability = 1.0 / ((maximum - topOffset) / (256.0 / IBigCube.DIAMETER_IN_BLOCKS));

        List<BlockPos> blockPosList = new ArrayList<>();
        while (probability > 0) {
            if (rand.nextDouble() > probability) {
                continue;
            }
            probability--;

            double maxBlockY = maximum - topOffset + config.bottomOffset;
            int blockY = rand.nextInt(IBigCube.DIAMETER_IN_BLOCKS) + minCubePos.getY()/*Equivalent to cubepos.getMinBlockY in 1.12*/;

            if (blockY > maxBlockY || blockY < bottomOffset) {
                continue;
            }
            blockPosList.add(new BlockPos(x, blockY, z));
        }
        return blockPosList.stream();
    }
}
