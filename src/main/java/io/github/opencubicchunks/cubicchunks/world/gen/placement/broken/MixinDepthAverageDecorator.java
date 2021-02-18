//package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;
//
//import java.util.Random;
//import java.util.stream.Stream;
//
//import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
//import io.github.opencubicchunks.cubicchunks.world.placement.CubicSimpleFeatureDecorator;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.levelgen.placement.DepthAverageConfigation;
//import net.minecraft.world.level.levelgen.placement.DepthAverageDecorator;
//import org.spongepowered.asm.mixin.Mixin;
//
//@Mixin(DepthAverageDecorator.class)
//public class MixinDepthAverageDecorator implements CubicSimpleFeatureDecorator<DepthAverageConfigation> {
//
//    @Override
//    public Stream<BlockPos> placeCubic(Random rand, DepthAverageConfigation config, BlockPos minCubePos) {
//        int baseline = config.baseline;
//        int spread = config.spread;
//        int x = minCubePos.getX();
//        int z = minCubePos.getZ();
//        int blockY = rand.nextInt(spread) + rand.nextInt(spread) - spread + baseline;
//
//        if (blockY < minCubePos.getY() || blockY >= minCubePos.getY() + IBigCube.DIAMETER_IN_BLOCKS) {
//            return Stream.of();
//        }
//
//        return Stream.of(new BlockPos(x, blockY, z));
//    }
//}
