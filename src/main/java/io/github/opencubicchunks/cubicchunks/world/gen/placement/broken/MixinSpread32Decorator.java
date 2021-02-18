//package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;
//
//import java.util.Random;
//import java.util.stream.Stream;
//
//import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
//import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;
//import net.minecraft.world.level.levelgen.placement.DecorationContext;
//import net.minecraft.world.level.levelgen.placement.Spread32Decorator;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(Spread32Decorator.class)
//public class MixinSpread32Decorator {
//
//
//    @Inject(at = @At("HEAD"),
//        method = "y",
//        cancellable = true)
//    private void getCubicPositions(DecorationContext decorationContext, Random random, NoneDecoratorConfiguration noneDecoratorConfiguration, int i, CallbackInfoReturnable<Integer> cir) {
//
//        CubicLevelHeightAccessor context = (CubicLevelHeightAccessor) decorationContext;
//
//        if (context.generates2DChunks()) {
//            return;
//        }
//
//        if (random.nextFloat() >= (0.125F * IBigCube.DIAMETER_IN_SECTIONS)) {
//            cir.setReturnValue(Stream.of());
//            return;
//        }
//        int x = blockPos.getX();
//        int z = blockPos.getZ();
//
//        int yHeightMap = blockPos.getY();
//        if (yHeightMap <= decorationContext.getMinBuildHeight()) {
//            cir.setReturnValue(Stream.of());
//        } else {
//            int y = blockPos.getY() + random.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
//            cir.setReturnValue(Stream.of(new BlockPos(x, y, z)));
//        }
//    }
//}
