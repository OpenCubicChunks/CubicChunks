//package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.stream.Stream;
//
//import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
//import io.github.opencubicchunks.cubicchunks.world.placement.CubicSimpleFeatureDecorator;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
//import net.minecraft.world.level.levelgen.placement.EmeraldPlacementDecorator;
//import org.spongepowered.asm.mixin.Mixin;
//
//@Mixin(EmeraldPlacementDecorator.class)
//public class MixinEmeraldPlacementDecorator<DC extends DecoratorConfiguration> implements CubicSimpleFeatureDecorator<DC> {
//
//    //Emerald ore is a self configured ore.
//    @Override
//    public Stream<BlockPos> placeCubic(Random rand, DC config, BlockPos minCubePos) {
//        List<BlockPos> blockPosList = new ArrayList<>();
//        int count = 3 + rand.nextInt(6);
//
//        for (int idx = 0; idx < count; idx++) {
//            int maximum = 32;
//            double bottomOffset = Double.NEGATIVE_INFINITY;
//            int topOffset = 4;
//
//            int x = minCubePos.getX() + rand.nextInt(16);
//            int z = minCubePos.getZ() + rand.nextInt(16);
//
//            double probability = 1.0 / ((maximum - topOffset) / (256.0 / IBigCube.DIAMETER_IN_BLOCKS));
//
//            while (probability > 0) {
//                if (rand.nextDouble() > probability) {
//                    continue;
//                }
//                probability--;
//
//                double maxBlockY = maximum - topOffset + 4;
//                int blockY = rand.nextInt(IBigCube.DIAMETER_IN_BLOCKS) + minCubePos.getY()/*Equivalent to cubepos.getMinBlockY in 1.12*/;
//
//                if (blockY > maxBlockY || blockY < bottomOffset) {
//                    continue;
//                }
//                blockPosList.add(new BlockPos(x, blockY, z));
//            }
//        }
//        return blockPosList.stream();
//    }
//}
