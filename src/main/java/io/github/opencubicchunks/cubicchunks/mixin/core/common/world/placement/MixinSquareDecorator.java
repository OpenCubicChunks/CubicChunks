package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.world.placement.CubicSimpleFeatureDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.SquareDecorator;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(SquareDecorator.class)
public abstract class MixinSquareDecorator implements CubicSimpleFeatureDecorator {

    @Override
    public Stream<BlockPos> placeCubic(Random rand, DecoratorConfiguration config, BlockPos minCubePos) {
        int x = rand.nextInt(IBigCube.DIAMETER_IN_BLOCKS) + minCubePos.getX();
        int y = minCubePos.getY();
        int z = rand.nextInt(IBigCube.DIAMETER_IN_BLOCKS) + minCubePos.getZ();
        return Stream.of(new BlockPos(x, y, z));
    }
}
